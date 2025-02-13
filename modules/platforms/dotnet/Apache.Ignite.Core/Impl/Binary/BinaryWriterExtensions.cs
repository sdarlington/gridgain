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

namespace Apache.Ignite.Core.Impl.Binary
{
    using System;
    using System.Collections.Generic;
    using System.Diagnostics;
    using System.IO;
    using Apache.Ignite.Core.Binary;

    /// <summary>
    /// Writer extensions.
    /// </summary>
    internal static class BinaryWriterExtensions
    {
        /// <summary>
        /// Write collection.
        /// </summary>
        /// <param name="writer">Writer.</param>
        /// <param name="vals">Values.</param>
        /// <param name="selector">A transform function to apply to each element.</param>
        /// <returns>The same writer for chaining.</returns>
        private static void WriteCollection<T1, T2>(this BinaryWriter writer, ICollection<T1> vals, 
            Func<T1, T2> selector)
        {
            writer.WriteInt(vals.Count);

            if (selector == null)
            {
                foreach (var val in vals)
                    writer.WriteObjectDetached(val);
            }
            else
            {
                foreach (var val in vals)
                    writer.WriteObjectDetached(selector(val));
            }
        }

        /// <summary>
        /// Write enumerable.
        /// </summary>
        /// <param name="writer">Writer.</param>
        /// <param name="vals">Values.</param>
        /// <returns>The same writer for chaining.</returns>
        public static void WriteEnumerable<T>(this BinaryWriter writer, IEnumerable<T> vals)
        {
            WriteEnumerable<T, T>(writer, vals, null);
        }

        /// <summary>
        /// Write enumerable.
        /// </summary>
        /// <param name="writer">Writer.</param>
        /// <param name="vals">Values.</param>
        /// <param name="selector">A transform function to apply to each element.</param>
        /// <returns>The same writer for chaining.</returns>
        public static void WriteEnumerable<T1, T2>(this BinaryWriter writer, IEnumerable<T1> vals, 
            Func<T1, T2> selector)
        {
            var col = vals as ICollection<T1>;

            if (col != null)
            {
                WriteCollection(writer, col, selector);
                return;
            }

            var stream = writer.Stream;

            var pos = stream.Position;

            stream.Seek(4, SeekOrigin.Current);

            var size = 0;

            if (selector == null)
            {
                foreach (var val in vals)
                {
                    writer.WriteObjectDetached(val);

                    size++;
                }
            }
            else
            {
                foreach (var val in vals)
                {
                    writer.WriteObjectDetached(selector(val));

                    size++;
                }
            }

            stream.WriteInt(pos, size);
        }

        /// <summary>
        /// Write dictionary.
        /// </summary>
        /// <param name="writer">Writer.</param>
        /// <param name="vals">Values.</param>
        public static void WriteDictionary<T1, T2>(this BinaryWriter writer, IEnumerable<KeyValuePair<T1, T2>> vals)
        {
            var pos = writer.Stream.Position;
            writer.WriteInt(0);  // Reserve count.

            int cnt = 0;

            foreach (var pair in vals)
            {
                writer.WriteObjectDetached(pair.Key);
                writer.WriteObjectDetached(pair.Value);

                cnt++;
            }

            writer.Stream.WriteInt(pos, cnt);
        }

        /// <summary>
        /// Writes the collection of write-aware items.
        /// </summary>
        public static void WriteCollectionRaw<T, TWriter>(this TWriter writer, ICollection<T> collection)
            where T : IBinaryRawWriteAware<TWriter> where TWriter: IBinaryRawWriter
        {
            WriteCollectionRaw(writer, collection, (w, x) => x.Write(w));
        }

        /// <summary>
        /// Writes the collection of write-aware items.
        /// </summary>
        public static void WriteCollectionRaw<T, TWriter>(this TWriter writer, ICollection<T> collection, Action<TWriter, T> action)
            where T : IBinaryRawWriteAware<TWriter> where TWriter: IBinaryRawWriter
        {
            Debug.Assert(writer != null);

            if (collection != null)
            {
                writer.WriteInt(collection.Count);

                foreach (var x in collection)
                {
                    if (x == null)
                    {
                        throw new ArgumentNullException(string.Format("{0} can not be null", typeof(T).Name));
                    }

                    action(writer, x);
                }
            }
            else
            {
                writer.WriteInt(0);
            }
        }

        /// <summary>
        /// Writes strings.
        /// </summary>
        /// <param name="writer">Writer.</param>
        /// <param name="strings">Strings.</param>
        public static int WriteStrings(this BinaryWriter writer, IEnumerable<string> strings)
        {
            Debug.Assert(writer != null);
            Debug.Assert(strings != null);
            
            var pos = writer.Stream.Position;

            var count = 0;
            writer.WriteInt(count);  // Reserve space.

            foreach (var cacheName in strings)
            {
                writer.WriteString(cacheName);
                count++;
            }

            writer.Stream.WriteInt(pos, count);

            return count;
        }
    }
}
