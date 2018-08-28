package edu.vt.cs.ir.examples;

import edu.vt.cs.ir.utils.LuceneUtils;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.util.Set;
import java.util.TreeSet;

/**
 * The is an example for transforming between internal and external IDs in Lucene indexes.
 *
 * @author Jiepu Jiang (jiepu@cs.vt.edu)
 * @version 2018-08-21
 */
public class LuceneExternalInternalIDs {

    public static void main( String[] args ) {
        try {

            // change to your index path
            String pathIndex = "/Users/jiepu/Downloads/example_index_lucene";

            // the name of the external ID (docno) field
            String fieldName = "docno";

            Directory dir = FSDirectory.open( new File( pathIndex ).toPath() );
            IndexReader index = DirectoryReader.open( dir );

            System.out.printf( "%-15s%-20s\n", "Internal ID", "External ID (docno)" );
            Set<String> docnos = new TreeSet<>();

            // Now we iteratively read and print out the internal and external IDs for documents in the index.
            // Lucene internal document ids are just non-negative integers.
            // You can get the maximum internal ID + 1 by calling index.maxDoc().
            // So you can iterate through all indexed documents by a simple for loop as follows.
            for ( int docid = 0; docid < index.maxDoc(); docid++ ) {
                String docno = LuceneUtils.getDocno( index, fieldName, docid );
                docnos.add( docno );
                System.out.printf( "%-15d%-20s\n", docid, docno );
            }

            // We can locate a document's internal ID by its external ID (docno) as well.
            // The following loop iterates through the docnos of documents and find their internal IDs.
            System.out.printf( "%-20s%-15s\n", "External ID (docno)", "Internal ID" );
            for ( String docno : docnos ) {
                int docid = LuceneUtils.findByDocno( index, fieldName, docno );
                System.out.printf( "%-20s%-15d\n", docno, docid );
            }

            index.close();
            dir.close();

        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

}
