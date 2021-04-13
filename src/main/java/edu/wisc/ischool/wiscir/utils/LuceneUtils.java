package edu.wisc.ischool.wiscir.utils;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiTerms;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Some utility functions.
 *
 * @author Jiepu Jiang (jiepu.jiang@wisc.edu)
 * @version 2021-04-12
 */
public class LuceneUtils {

    /**
     * Find a document in the index by its docno (external ID).
     * Returns the internal ID of the document; or -1 if not found.
     *
     * @param index      An index reader.
     * @param fieldDocno The name of the field you used for storing docnos (external document IDs).
     * @param docno      The docno (external ID) you are looking for.
     * @return The internal ID of the document in the index; or -1 if not found.
     * @throws IOException
     */
    public static int findByDocno( IndexReader index, String fieldDocno, String docno ) throws IOException {
        BytesRef term = new BytesRef( docno );
        PostingsEnum posting = MultiTerms.getTermPostingsEnum( index, fieldDocno, term, PostingsEnum.NONE );
        if ( posting != null ) {
            int docid = posting.nextDoc();
            if ( docid != PostingsEnum.NO_MORE_DOCS ) {
                return docid;
            }
        }
        return -1;
    }

    /**
     * Get the DocNo (external ID) of a document stored in the index by its internal id.
     *
     * @param index      An index reader.
     * @param fieldDocno The name of the field you used for storing docnos (external document IDs).
     * @param docid      The internal ID of the document
     * @return The docno (external ID) of the document.
     * @throws IOException
     */
    public static String getDocno( IndexReader index, String fieldDocno, int docid ) throws IOException {
        // This implementation is just for you to quickly understand how this works.
        // You should consider reuse the fieldset if you need to read docnos for a lot of documents.
        Set<String> fieldset = new HashSet<>();
        fieldset.add( fieldDocno );
        Document d = index.document( docid, fieldset );
        return d.get( fieldDocno );
    }

}
