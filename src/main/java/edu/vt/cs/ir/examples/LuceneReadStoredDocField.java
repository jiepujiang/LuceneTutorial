package edu.vt.cs.ir.examples;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * In Lucene, we can store contents in an index document just let a database.
 *
 * @author Jiepu Jiang (jiepu@cs.vt.edu)
 * @version 2018-08-21
 */
public class LuceneReadStoredDocField {

    public static void main( String[] args ) {
        try {

            String pathIndex = "/Users/jiepu/Downloads/example_index_lucene";

            // Let's just retrieve the docno (external ID) and title of the first 100 documents in the index

            // store the names of the fields you hope to access in the following set
            Set<String> fieldset = new HashSet<>();
            fieldset.add( "docno" );
            fieldset.add( "title" );
            // You can add more fields into the set as long as they have been stored at indexing time.
            // But for efficiency issues, you should only include the fields you are going to use.

            Directory dir = FSDirectory.open( new File( pathIndex ).toPath() );
            IndexReader index = DirectoryReader.open( dir );

            System.out.printf( "%-10s%-15s%-30s\n", "DOCID", "DOCNO", "TITLE" );

            // iteratively read and print out the first 100 documents' internal IDs, external IDs (DOCNOs), and titles.
            for ( int docid = 0; docid < index.maxDoc() && docid < 100; docid++ ) {

                // The following line retrieves a stored document representation from the index.
                // It will only retrieve the fields you included in the set.
                // There is also a method ixreader.document( docid ), which simply retrieves all the stored fields for a document.
                Document doc = index.document( docid, fieldset );

                // Now you can get the string data previously stored in the field of the document.
                // Again, you can only access the data field's value if you stored the value at index time.
                String docno = doc.getField( "docno" ).stringValue();
                String title = doc.getField( "title" ).stringValue();

                // You can also store other types of data (such as integers, floats, bytes, etc) in an indexed document.
                // You can access the data using methods such as field.numericValue(), field.binaryValue(), etc.

                System.out.printf( "%-10d%-15s%-30s\n", docid, docno, title );
            }

            index.close();
            dir.close();

        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

}
