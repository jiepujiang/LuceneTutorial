package edu.vt.cs.ir.examples;

import org.apache.lucene.index.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.File;

/**
 * This is an example for iterating the vocabulary of an index (and accessing corpus-level statistics of terms).
 *
 * @author Jiepu Jiang (jiepu@cs.vt.edu)
 * @version 2018-08-21
 */
public class LuceneIterateVocabulary {

    public static void main( String[] args ) {
        try {

            String pathIndex = "/Users/jiepu/Downloads/example_index_lucene";

            // Let's just retrieve the vocabulary of the "text" field
            String field = "text";

            Directory dir = FSDirectory.open( new File( pathIndex ).toPath() );
            IndexReader index = DirectoryReader.open( dir );

            double N = index.numDocs();
            double corpusLength = index.getSumTotalTermFreq( field );

            System.out.printf( "%-30s%-10s%-10s%-10s%-10s\n", "TERM", "DF", "TOTAL_TF", "IDF", "p(w|c)" );

            // Get the vocabulary of the index.
            Terms voc = MultiFields.getTerms( index, field );
            // You need to use TermsEnum to iterate each entry of the vocabulary.
            TermsEnum termsEnum = voc.iterator();
            BytesRef term;
            int count = 0;
            while ( ( term = termsEnum.next() ) != null ) {
                count++;
                String termstr = term.utf8ToString(); // get the text string of the term
                int df = termsEnum.docFreq(); // get the document frequency (DF) of the term
                long freq = termsEnum.totalTermFreq(); // get the total frequency of the term
                double idf = Math.log( ( N + 1 ) / ( df + 1 ) );
                double pwc = freq / corpusLength;
                System.out.printf( "%-30s%-10d%-10d%-10.2f%-10.8f\n", termstr, df, freq, idf, pwc );
                if ( count >= 100 ) {
                    break;
                }
            }

            index.close();
            dir.close();

        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

}
