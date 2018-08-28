package edu.vt.cs.ir.utils;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.KStemFilter;
import org.apache.lucene.analysis.snowball.SnowballFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharTokenizer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.util.AttributeFactory;
import org.apache.lucene.util.BytesRef;
import org.tartarus.snowball.ext.EnglishStemmer;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Some Lucene utilities for CS 5604 students.
 *
 * @author Jiepu Jiang (jiepu@cs.vt.edu)
 * @version 2018-08-27
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
        PostingsEnum posting = MultiFields.getTermDocsEnum( index, fieldDocno, term, PostingsEnum.NONE );
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

    /**
     * Get the value of a stored document field in the index by docid.
     * Note that you need to store the value of the field at indexing time.
     *
     * @param index An index reader.
     * @param field The name of the field.
     * @param docid The internal ID of the document
     * @return The value of the stored document field.
     * @throws IOException
     */
    public static String getDocFieldValue( IndexReader index, String field, int docid ) throws IOException {
        // This implementation is just for you to quickly understand how this works.
        // You should consider reuse the fieldset if you need to read docnos for a lot of documents.
        Set<String> fieldset = new HashSet<>();
        fieldset.add( field );
        Document d = index.document( docid, fieldset );
        return d.get( field );
    }

    /**
     * Stemming options: NoStemming, Krovetz, or Porter2 (English snowball).
     */
    public enum Stemming {
        NoStemming,
        Krovetz,
        Porter2
    }

    /**
     * Get a Lucene text analyzer that: 1) uses StandardTokenizer to split texts into tokens;
     * 2) lowercases all letters; 3) applies the specified stemming option; and 4) does not remove stop words.
     *
     * @param stemming
     * @return
     */
    public static Analyzer getAnalyzer( final Stemming stemming ) {
        return new Analyzer() {
            protected TokenStreamComponents createComponents( String fieldName ) {
                TokenStreamComponents ts = new TokenStreamComponents( new StandardTokenizer() );
                ts = new TokenStreamComponents( ts.getTokenizer(), new LowerCaseFilter( ts.getTokenStream() ) );
                if ( stemming == Stemming.Krovetz ) {
                    ts = new TokenStreamComponents( ts.getTokenizer(), new KStemFilter( ts.getTokenStream() ) );
                } else if ( stemming == Stemming.Porter2 ) {
                    ts = new TokenStreamComponents( ts.getTokenizer(), new SnowballFilter( ts.getTokenStream(), "English" ) );
                }
                return ts;
            }
        };
    }

    /**
     * Tokenize the input text into a list of tokens/words using a Lucene analyzer.
     *
     * @param text     An input text.
     * @param analyzer An Analyzer.
     * @return A list of tokenized words.
     */
    public static List<String> tokenize( String text, Analyzer analyzer ) throws IOException {
        List<String> tokens = new ArrayList<>();
        TokenStream ts = analyzer.tokenStream( "", new StringReader( text ) );
        CharTermAttribute attr = ts.getAttribute( CharTermAttribute.class );
        ts.reset();
        while ( ts.incrementToken() ) {
            tokens.add( attr.toString() );
        }
        ts.end();
        ts.close();
        return tokens;
    }

    private static final EnglishStemmer porter2 = new EnglishStemmer();

    /**
     * @param word A word.
     * @return Its Porter2 stem.
     */
    public static String stemPorter2( String word ) {
        porter2.setCurrent( word );
        porter2.stem();
        return porter2.getCurrent();
    }

    private static final class DoNothingTokenizer extends CharTokenizer {

        public DoNothingTokenizer() {
        }

        public DoNothingTokenizer( AttributeFactory factory ) {
            super( factory );
        }

        protected boolean isTokenChar( int c ) {
            return true;
        }
    }

    private static Analyzer kstemmer = new Analyzer() {
        protected TokenStreamComponents createComponents( String fieldName ) {
            TokenStreamComponents ts = new TokenStreamComponents( new DoNothingTokenizer() );
            ts = new TokenStreamComponents( ts.getTokenizer(), new KStemFilter( ts.getTokenStream() ) );
            return ts;
        }
    };

    /**
     * Well, this is kind of slow and probably not thread-safe,
     * but Lucene's Krovetz stemmer doesn't have a public constructor.
     * You can create a more efficient Krovertz stemmer by copy & paste Lucene's own implementations.
     *
     * @param word A word.
     * @return Its Krovetz stem.
     * @throws IOException
     */
    public static String stemKrovetz( String word ) throws IOException {
        StringBuilder sb = new StringBuilder();
        TokenStream ts = kstemmer.tokenStream( "", new StringReader( word ) );
        CharTermAttribute attr = ts.getAttribute( CharTermAttribute.class );
        ts.reset();
        while ( ts.incrementToken() ) {
            sb.append( attr.toString() );
        }
        ts.end();
        ts.close();
        return sb.toString();
    }

}
