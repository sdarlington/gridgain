/*
 * Copyright 2019 GridGain Systems, Inc. and Contributors.
 *
 * Licensed under the GridGain Community Edition License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.gridgain.com/products/software/community-edition/gridgain-community-edition-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.internal.processors.cache.persistence;

import java.util.function.Supplier;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.internal.metric.IoStatisticsHolder;
import org.apache.ignite.internal.pagemem.PageIdUtils;
import org.apache.ignite.internal.pagemem.PageMemory;
import org.apache.ignite.internal.processors.cache.CacheGroupContext;
import org.apache.ignite.internal.processors.cache.CacheObjectContext;
import org.apache.ignite.internal.processors.cache.GridCacheSharedContext;
import org.apache.ignite.internal.processors.cache.persistence.freelist.FreeList;
import org.apache.ignite.internal.processors.cache.persistence.tree.util.PageHandler;
import org.apache.ignite.internal.processors.query.GridQueryRowCacheCleaner;
import org.apache.ignite.internal.util.typedef.internal.U;

/**
 * Data store for H2 rows.
 */
public class RowStore {
    /** */
    private final FreeList freeList;

    /** */
    private final GridCacheSharedContext ctx;

    /** */
    protected final PageMemory pageMem;

    /** */
    protected final CacheObjectContext coctx;

    /** */
    private final boolean persistenceEnabled;

    /** Row cache cleaner. */
    private volatile Supplier<GridQueryRowCacheCleaner> rowCacheCleaner = () -> null;

    /** */
    protected final CacheGroupContext grp;

    /**
     * @param grp Cache group.
     * @param freeList Free list.
     */
    public RowStore(CacheGroupContext grp, FreeList freeList) {
        assert grp != null;
        assert freeList != null;

        this.grp = grp;
        this.freeList = freeList;

        ctx = grp.shared();
        coctx = grp.cacheObjectContext();
        pageMem = grp.dataRegion().pageMemory();

        persistenceEnabled = grp.dataRegion().config().isPersistenceEnabled();
    }

    /**
     * @param link Row link.
     * @throws IgniteCheckedException If failed.
     */
    public void removeRow(long link, IoStatisticsHolder statHolder) throws IgniteCheckedException {
        assert link != 0;

        GridQueryRowCacheCleaner rowCacheCleaner0 = rowCacheCleaner.get();

        if (rowCacheCleaner0 != null)
            rowCacheCleaner0.remove(link);

        if (!persistenceEnabled)
            freeList.removeDataRowByLink(link, statHolder);
        else {
            ctx.database().checkpointReadLock();

            try {
                freeList.removeDataRowByLink(link, statHolder);
            }
            finally {
                ctx.database().checkpointReadUnlock();
            }
        }
    }

    /**
     * @param row Row.
     * @throws IgniteCheckedException If failed.
     */
    public void addRow(CacheDataRow row, IoStatisticsHolder statHolder) throws IgniteCheckedException {
        if (!persistenceEnabled) {
            ctx.database().ensureFreeSpaceForInsert(grp.dataRegion(), row);

            freeList.insertDataRow(row, statHolder);
        }
        else {
            ctx.database().checkpointReadLock();

            try {
                freeList.insertDataRow(row, statHolder);

                assert row.link() != 0L;
            }
            finally {
                ctx.database().checkpointReadUnlock();
            }
        }

        assert row.key().partition() == PageIdUtils.partId(row.link()) :
            "Constructed a link with invalid partition ID [partId=" + row.key().partition() +
                ", link=" + U.hexLong(row.link()) + ']';
    }

    /**
     * @param link Row link.
     * @param row New row data.
     * @return {@code True} if was able to update row.
     * @throws IgniteCheckedException If failed.
     */
    public boolean updateRow(long link, CacheDataRow row, IoStatisticsHolder statHolder) throws IgniteCheckedException {
        assert !persistenceEnabled || ctx.database().checkpointLockIsHeldByThread();

        GridQueryRowCacheCleaner rowCacheCleaner0 = rowCacheCleaner.get();

        if (rowCacheCleaner0 != null)
            rowCacheCleaner0.remove(link);

        return freeList.updateDataRow(link, row, statHolder);
    }

    /**
     * Run page handler operation over the row.
     *
     * @param link Row link.
     * @param pageHnd Page handler.
     * @param arg Page handler argument.
     * @throws IgniteCheckedException If failed.
     */
    public <S, R> void updateDataRow(long link, PageHandler<S, R> pageHnd, S arg,
        IoStatisticsHolder statHolder) throws IgniteCheckedException {
        if (!persistenceEnabled)
            freeList.updateDataRow(link, pageHnd, arg, statHolder);
        else {
            ctx.database().checkpointReadLock();

            try {
                freeList.updateDataRow(link, pageHnd, arg, statHolder);
            }
            finally {
                ctx.database().checkpointReadUnlock();
            }
        }
    }

    /**
     * @return Free list.
     */
    public FreeList freeList() {
        return freeList;
    }

    /**
     * Inject rows cache cleaner.
     *
     * @param rowCacheCleaner Rows cache cleaner.
     */
    public void setRowCacheCleaner(Supplier<GridQueryRowCacheCleaner> rowCacheCleaner) {
        assert rowCacheCleaner != null;

        this.rowCacheCleaner = rowCacheCleaner;
    }
}
