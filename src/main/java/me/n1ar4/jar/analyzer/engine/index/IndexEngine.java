/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.engine.index;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import me.n1ar4.jar.analyzer.engine.index.entity.Result;
import me.n1ar4.jar.analyzer.gui.LuceneSearchForm;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.codecs.lucene50.Lucene50StoredFieldsFormat;
import org.apache.lucene.codecs.lucene70.Lucene70Codec;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;


public class IndexEngine {
    public static void refreshSearcher() {
        IndexSingletonClass.refreshSearcher();
    }

    public static void ensureWriter(IndexWriterConfig.OpenMode openMode) throws IOException {
        IndexSingletonClass.getIndexWriter(openMode);
    }

    public static void closeAll() {
        IndexSingletonClass.closeAll();
    }

    public static String initIndex(Map<String, String> analyzerMap) throws IOException {
        return initIndex(analyzerMap, IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
    }

    public static String initIndex(Map<String, String> analyzerMap,
                                   IndexWriterConfig.OpenMode openMode) throws IOException {
        IndexWriter indexWriter = IndexSingletonClass.getIndexWriter(openMode);

        analyzerMap.forEach((key, value) -> {
            Collection<Document> documents = new ArrayList<>();
            String[] cut = StrUtil.cut(StrUtil.cleanBlank(StrUtil.removeAllLineBreaks(value)), 1800);
            for (String string : cut) {
                addDoc(key, string, documents);
            }
            try {
                indexWriter.addDocuments(documents);
                indexWriter.commit();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        return null;
    }

    private static void addDoc(String key, String string, Collection<Document> documents) {
        Document doc = new Document();
        doc.add(new StringField("content", string, Field.Store.YES));
        doc.add(new StringField("content_lower", string.toLowerCase(), Field.Store.YES));
        doc.add(new StringField("codePath", key, Field.Store.YES));
        doc.add(new StringField("title",
                StrUtil.removeSuffix(FileUtil.getName(key), ".class"), Field.Store.YES));
        documents.add(doc);
    }


    public static Result searchNormal(String keyword) throws IOException {
        if (StrUtil.isBlank(keyword)) {
            Result result = new Result();
            result.setTotal(0L);
            result.setData(new ArrayList<>());
            return result;
        }
        IndexReader reader = IndexSingletonClass.getReader();
        keyword = StrUtil.removeAllLineBreaks(keyword);
        //区分/忽略大小写查询
        Query query = new WildcardQuery(new Term(LuceneSearchForm.useCaseSensitive() ?
                "content" : "content_lower", LuceneSearchForm.useCaseSensitive() ?
                "*" + StrUtil.removeAllLineBreaks(StrUtil.cleanBlank(keyword)) + "*"
                : "*" + StrUtil.removeAllLineBreaks(StrUtil.cleanBlank(keyword.toLowerCase())) + "*"));
        return getResult(reader, IndexSingletonClass.getSearcher().search(query, 110));
    }

    public static Result searchRegex(String keyword) throws IOException {
        if (StrUtil.isBlank(keyword)) {
            Result result = new Result();
            result.setTotal(0L);
            result.setData(new ArrayList<>());
            return result;
        }
        IndexReader reader = IndexSingletonClass.getReader();
        keyword = StrUtil.removeAllLineBreaks(keyword);
        //区分/忽略大小写查询
        RegexpQuery query = new RegexpQuery(new Term(LuceneSearchForm.useCaseSensitive() ?
                "content" : "content_lower", LuceneSearchForm.useCaseSensitive() ?
                ".*" + Pattern.quote(StrUtil.removeAllLineBreaks(StrUtil.cleanBlank(keyword))) + ".*"
                : ".*" + Pattern.quote(StrUtil.removeAllLineBreaks(StrUtil.cleanBlank(keyword.toLowerCase()))) + ".*"));
        return getResult(reader, IndexSingletonClass.getSearcher().search(query, 110));
    }

    @NotNull
    private static Result getResult(IndexReader reader, TopDocs search) throws IOException {

        ScoreDoc[] scoreDocs = search.scoreDocs;
        List<Map<String, Object>> arrayList = new ArrayList<>();
        for (ScoreDoc scoreDoc : scoreDocs) {
            int docID = scoreDoc.doc;
            Document doc = reader.document(docID);
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("path", doc.get("codePath"));
            map.put("content", doc.get("content"));
            map.put("title", doc.get("title"));
            arrayList.add(map);
        }
        Result result = new Result();
        result.setTotal(search.totalHits);
        result.setData(arrayList);
        return result;
    }

    public static class IndexSingletonClass {
        private static volatile IndexSearcher searcher = null;
        private static volatile IndexReader reader = null;
        private static volatile IndexWriter indexWriter = null;

        public static IndexSearcher getSearcher() throws IOException {
            if (searcher == null) {
                synchronized (IndexSingletonClass.class) {
                    if (searcher == null) {
                        IndexReader reader1 = getReader();
                        searcher = new IndexSearcher(reader1);
                    }
                }
            }
            return searcher;
        }

        public static IndexReader getReader() throws IOException {
            if (reader == null) {
                synchronized (IndexSingletonClass.class) {
                    if (reader == null) {
                        Directory directory = FSDirectory.open(Paths.get(IndexPluginsSupport.DocumentPath));
                        reader = DirectoryReader.open(directory);
                    }
                }
            }
            return reader;
        }

        public static IndexWriter getIndexWriter() throws IOException {
            return getIndexWriter(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        }

        public static IndexWriter getIndexWriter(IndexWriterConfig.OpenMode openMode) throws IOException {
            if (indexWriter == null) {
                synchronized (IndexSingletonClass.class) {

                    if (indexWriter == null) {
                        Directory directory = FSDirectory.open(Paths.get(IndexPluginsSupport.DocumentPath));
                        IndexWriterConfig conf = new IndexWriterConfig(new StandardAnalyzer());
                        conf.setOpenMode(openMode);
                        //优化了索引文件编码，提高存储效率
                        conf.setCodec(new Lucene70Codec(Lucene50StoredFieldsFormat.Mode.BEST_COMPRESSION));
                        indexWriter = new IndexWriter(directory, conf);
                    }
                }
            }
            return indexWriter;
        }

        public static void refreshSearcher() {
            synchronized (IndexSingletonClass.class) {
                if (reader == null) {
                    return;
                }
                try {
                    if (reader instanceof DirectoryReader) {
                        DirectoryReader newReader = DirectoryReader.openIfChanged((DirectoryReader) reader);
                        if (newReader != null) {
                            IndexReader old = reader;
                            reader = newReader;
                            searcher = new IndexSearcher(reader);
                            old.close();
                        }
                    } else {
                        Directory directory = FSDirectory.open(Paths.get(IndexPluginsSupport.DocumentPath));
                        IndexReader old = reader;
                        reader = DirectoryReader.open(directory);
                        searcher = new IndexSearcher(reader);
                        old.close();
                    }
                } catch (IOException ignored) {
                }
            }
        }

        public static void closeAll() {
            synchronized (IndexSingletonClass.class) {
                try {
                    if (indexWriter != null) {
                        indexWriter.close();
                    }
                } catch (IOException ignored) {
                }
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException ignored) {
                }
                indexWriter = null;
                reader = null;
                searcher = null;
            }
        }
    }
}
