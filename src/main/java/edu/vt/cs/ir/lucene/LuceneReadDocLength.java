package edu.vt.cs.ir.lucene;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * This is an example of counting document field length in Lucene.
 * Unfortunately, by the time I created the tutorial, Lucene does not store document length in its index.
 * An acceptable but slow solution is that you calculate document length by yourself based on a document
 * vector. In case your dataset is static and relatively small (such as just about or less than a few
 * million documents), you can simply compute all documents' lengths after you've built an index and store
 * them in an external file (it takes just 4MB to store 1 million docs' lengths as integers). At running
 * time, you can load all the computed document lengths to avoid loading doc vector and computing length.
 *
 * @author Jiepu Jiang (jiepu@cs.vt.edu)
 * @version 2018-08-21
 */
public class LuceneReadDocLength {

    public static void main( String[] args ) {
        try {

            String pathIndex = "/Users/jiepu/Downloads/example_index_lucene";
            String field = "text";

            Directory dir = FSDirectory.open( new File( pathIndex ).toPath() );
            IndexReader ixreader = DirectoryReader.open( dir );

            // we also print out external ID
            Set<String> fieldset = new HashSet<>();

            fieldset.add( "docno" );

            // The following loop iteratively print the lengths of the documents in the index.
            System.out.printf( "%-10s%-15s%-10s\n", "DOCID", "DOCNO", "Length" );
            for ( int docid = 0; docid < ixreader.maxDoc(); docid++ ) {
                String docno = ixreader.document( docid, fieldset ).get( "docno" );
                int doclen = 0;
                // Unfortunately, Lucene does not store document length in its index
                // (because its retrieval model does not rely on document length).
                // An acceptable but slow solution is that you calculate document length by yourself based on
                // document vector. In case your dataset is static and relatively small (such as about or less
                // than a few million documents), you can simply compute the document lengths and store them in
                // an external file (it takes just a few MB). At running time, you can load all the computed
                // document lengths to avoid loading doc vector and computing length.
                TermsEnum termsEnum = ixreader.getTermVector( docid, field ).iterator();
                while ( termsEnum.next() != null ) {
                    doclen += termsEnum.totalTermFreq();
                }
                System.out.printf( "%-10d%-15s%-10d\n", docid, docno, doclen );
            }

            ixreader.close();
            dir.close();

        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

}
