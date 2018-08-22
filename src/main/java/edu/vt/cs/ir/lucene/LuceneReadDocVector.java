package edu.vt.cs.ir.lucene;

import org.apache.lucene.index.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.File;

/**
 * This is an example for accessing a stored document vector from a Lucene index.
 *
 * @author Jiepu Jiang (jiepu@cs.vt.edu)
 * @version 2018-08-21
 */
public class LuceneReadDocVector {
	
	public static void main( String[] args ) {
		try {
			
			String pathIndex = "/Users/jiepu/Downloads/example_index_lucene";
			
			// let's just retrieve the document vector (only the "text" field) for the Document with internal ID=21
			String field = "text";
			int docid = 21;
			
			Directory dir = FSDirectory.open( new File( pathIndex ).toPath() );
			IndexReader index = DirectoryReader.open( dir );
			
			Terms vector = index.getTermVector( docid, field ); // Read the document's document vector.
			
			// You need to use TermsEnum to iterate each entry of the document vector (in alphabetical order).
			System.out.printf( "%-20s%-10s%-20s\n", "TERM", "FREQ", "POSITIONS" );
			TermsEnum terms = vector.iterator();
			PostingsEnum positions = null;
			BytesRef term;
			while ( ( term = terms.next() ) != null ) {
				
				String termstr = term.utf8ToString(); // Get the text string of the term.
				long freq = terms.totalTermFreq(); // Get the frequency of the term in the document.
				
				System.out.printf( "%-20s%-10d", termstr, freq );
				
				// Lucene's document vector can also provide the position of the terms
				// (in case you stored these information in the index).
				// Here you are getting a PostingsEnum that includes only one document entry, i.e., the current document.
				positions = terms.postings( positions, PostingsEnum.POSITIONS );
				positions.nextDoc(); // you still need to move the cursor
				// now accessing the occurrence position of the terms by iteratively calling nextPosition()
				for ( int i = 0; i < freq; i++ ) {
					System.out.print( ( i > 0 ? "," : "" ) + positions.nextPosition() );
				}
				System.out.println();
				
			}
			
			index.close();
			dir.close();
			
		} catch ( Exception e ) {
			e.printStackTrace();
		}
	}
	
}
