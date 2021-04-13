package edu.wisc.ischool.wiscir.examples;

import org.apache.lucene.index.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * This is an example for accessing a term-document-position posting list from a Lucene index.
 *
 * @author Jiepu Jiang (jiepu.jiang@wisc.edu)
 * @version 2021-04-12
 */
public class LuceneReadPositionPosting {

    public static void main( String[] args ) {
        try {

            String pathIndex = "/home/jiepu/Downloads/example_index_lucene";

            // Let's just retrieve the posting list for the term "reformulation" in the "text" field
            String field = "text";
            String term = "reformulation";

            Directory dir = FSDirectory.open( new File( pathIndex ).toPath() );
            IndexReader index = DirectoryReader.open( dir );

            // we also print out external ID
            Set<String> fieldset = new HashSet<>();
            fieldset.add( "docno" );

            // The following line reads the posting list of the term in a specific index field.
            // You need to encode the term into a BytesRef object,
            // which is the internal representation of a term used by Lucene.
            System.out.printf( "%-10s%-15s%-10s%-20s\n", "DOCID", "DOCNO", "FREQ", "POSITIONS" );
            PostingsEnum posting = MultiTerms.getTermPostingsEnum( index, field, new BytesRef( term ), PostingsEnum.POSITIONS );
            if ( posting != null ) { // if the term does not appear in any document, the posting object may be null
                int docid;
                // Each time you call posting.nextDoc(), it moves the cursor of the posting list to the next position
                // and returns the docid of the current entry (document). Note that this is an internal Lucene docid.
                // It returns PostingsEnum.NO_MORE_DOCS if you have reached the end of the posting list.
                while ( ( docid = posting.nextDoc() ) != PostingsEnum.NO_MORE_DOCS ) {
                    String docno = index.document( docid, fieldset ).get( "docno" );
                    int freq = posting.freq(); // get the frequency of the term in the current document
                    System.out.printf( "%-10d%-15s%-10d", docid, docno, freq );
                    for ( int i = 0; i < freq; i++ ) {
                        // Get the next occurrence position of the term in the current document.
                        // Note that you need to make sure by yourself that you at most call this function freq() times.
                        System.out.print( ( i > 0 ? "," : "" ) + posting.nextPosition() );
                    }
                    System.out.println();
                }
            }

            index.close();
            dir.close();

        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

}
