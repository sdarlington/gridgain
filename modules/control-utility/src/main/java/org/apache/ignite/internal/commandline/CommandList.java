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

package org.apache.ignite.internal.commandline;

import org.apache.ignite.internal.commandline.cache.CacheCommands;
import org.apache.ignite.internal.commandline.diagnostic.DiagnosticCommand;
import org.apache.ignite.internal.commandline.dr.DrCommand;
import org.apache.ignite.internal.commandline.encryption.EncryptionCommands;
import org.apache.ignite.internal.commandline.meta.MetadataCommand;
import org.apache.ignite.internal.commandline.metric.MetricCommand;
import org.apache.ignite.internal.commandline.property.PropertyCommand;
import org.apache.ignite.internal.commandline.ru.RollingUpgradeCommand;

/**
 * High-level commands.
 */
public enum CommandList {
    /** */
    ACTIVATE("--activate", new ActivateCommand()),

    /** */
    DEACTIVATE("--deactivate", new DeactivateCommand()),

    /** */
    STATE("--state", new StateCommand()),

    /** */
    SET_STATE("--set-state", new ClusterStateChangeCommand()),

    /** */
    BASELINE("--baseline", new BaselineCommand()),

    /** */
    TX("--tx", new TxCommands()),

    /** */
    CACHE("--cache", new CacheCommands()),

    /** */
    WAL("--wal", new WalCommands()),

    /** */
    DIAGNOSTIC("--diagnostic", new DiagnosticCommand()),

    /** Encryption features command. */
    ENCRYPTION("--encryption", new EncryptionCommands()),

    /** */
    ROLLING_UPGRADE("--rolling-upgrade", new RollingUpgradeCommand()),

    /** */
    CLUSTER_CHANGE_TAG("--change-tag", new ClusterChangeTagCommand()),

    /** */
    CLUSTER_CHANGE_ID("--change-id", new ClusterChangeIdCommand()),

    /** */
    DATA_CENTER_REPLICATION("--dr", new DrCommand()),

    /** */
    TRACING_CONFIGURATION("--tracing-configuration", new TracingConfigurationCommand()),

    /** */
    SHUTDOWN_POLICY("--shutdown-policy", new ShutdownPolicyCommand()),

    /** */
    METADATA("--meta", new MetadataCommand()),

    /** Warm-up command. */
    WARM_UP("--warm-up", new WarmUpCommand()),

    /** Commands to manage distributed properties. */
    PROPERTY("--property", new PropertyCommand()),

    /** Command for printing metric values. */
    METRIC("--metric", new MetricCommand()),

    /** */
    PERSISTENCE("--persistence", new PersistenceCommand()),

    /** Command to manage PDS defragmentation. */
    DEFRAGMENTATION("--defragmentation", new DefragmentationCommand()),

    /** Start checkpoint on a cluster */
    CHECKPOINT("--checkpoint", new CheckpointCommand());

    /** Private values copy so there's no need in cloning it every time. */
    private static final CommandList[] VALUES = CommandList.values();

    /** */
    private final String text;

    /** Command implementation. */
    private final Command command;

    /**
     * @param text Text.
     * @param command Command implementation.
     */
    CommandList(String text, Command command) {
        this.text = text;
        this.command = command;
    }

    /**
     * @param text Command text.
     * @return Command for the text.
     */
    public static CommandList of(String text) {
        for (CommandList cmd : VALUES) {
            if (cmd.text().equalsIgnoreCase(text))
                return cmd;
        }

        return null;
    }

    /**
     * @return Command text.
     */
    public String text() {
        return text;
    }

    /**
     * @return Command implementation.
     */
    public Command command() {
        return command;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return text;
    }

    /**
     * @return command name
     */
    public String toCommandName() {
        return text.substring(2).toUpperCase();
    }
}
