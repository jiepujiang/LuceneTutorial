package edu.vt.cs.ir.examples;

import edu.vt.cs.ir.utils.LuceneUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.File;

/**
 * This is an example of accessing corpus statistics and corpus-level term statistics.
 *
 * @author Jiepu Jiang (jiepu@cs.vt.edu)
 * @version 2018-08-21
 */
public class LuceneSearchExample {

    public static void main( String[] args ) {
        try {

            String pathIndex = "/Users/jiepu/Downloads/example_index_lucene";

            // Just like building an index, we also need an Analyzer to process the query strings
            Analyzer analyzer = new Analyzer() {
                @Override
                protected TokenStreamComponents createComponents( String fieldName ) {
                    // Step 1: tokenization (Lucene's StandardTokenizer is suitable for most text retrieval occasions)
                    TokenStreamComponents ts = new TokenStreamComponents( new StandardTokenizer() );
                    // Step 2: transforming all tokens into lowercased ones (recommended for the majority of the problems)
                    ts = new TokenStreamComponents( ts.getTokenizer(), new LowerCaseFilter( ts.getTokenStream() ) );
                    // Step 3: whether to remove stop words
                    // Uncomment the following line to remove stop words
                    // ts = new TokenStreamComponents( ts.getTokenizer(), new StopFilter( ts.getTokenStream(), StandardAnalyzer.ENGLISH_STOP_WORDS_SET ) );
                    // Step 4: whether to apply stemming
                    // Uncomment the following line to apply Krovetz or Porter stemmer
                    // ts = new TokenStreamComponents( ts.getTokenizer(), new KStemFilter( ts.getTokenStream() ) );
                    // ts = new TokenStreamComponents( ts.getTokenizer(), new PorterStemFilter( ts.getTokenStream() ) );
                    return ts;
                }
            };

            String field = "text"; // the field you hope to search for
            QueryParser parser = new QueryParser( field, analyzer ); // a query parser that transforms a text string into Lucene's query object

            String qstr = "query reformulation"; // this is the textual search query
            Query query = parser.parse( qstr ); // this is Lucene's query object

            // Okay, now let's open an index and search for documents
            Directory dir = FSDirectory.open( new File( pathIndex ).toPath() );
            IndexReader index = DirectoryReader.open( dir );

            // you need to create a Lucene searcher
            IndexSearcher searcher = new IndexSearcher( index );

            // Lucene's default ranking model is VSM, but it has also implemented a wide variety of retrieval models.
            // Tell Lucene to rank results using the BM25 retrieval model.
            // Note that Lucene's implementation of BM25 is somehow different from the one we'll cover in class.
            searcher.setSimilarity( new BM25Similarity() );

            int top = 10; // Let's just retrieve the talk 10 results
            TopDocs docs = searcher.search( query, top ); // retrieve the top 10 results; retrieved results are stored in TopDocs

            System.out.printf( "%-10s%-20s%-10s%s\n", "Rank", "DocNo", "Score", "Title" );
            int rank = 1;
            for ( ScoreDoc scoreDoc : docs.scoreDocs ) {
                int docid = scoreDoc.doc;
                double score = scoreDoc.score;
                String docno = LuceneUtils.getDocno( index, "docno", docid );
                String title = LuceneUtils.getDocno( index, "title", docid );
                System.out.printf( "%-10d%-20s%-10.4f%s\n", rank, docno, score, title );
                rank++;
            }

            // remember to close the index and the directory
            index.close();
            dir.close();

        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

}
