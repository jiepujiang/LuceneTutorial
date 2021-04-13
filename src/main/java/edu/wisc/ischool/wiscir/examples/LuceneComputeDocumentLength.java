package edu.wisc.ischool.wiscir.examples;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class LuceneComputeDocumentLength {

    public static void main( String[] args ) {
        try {
            String pathIndex = "/home/jiepu/Downloads/example_index_lucene";
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
