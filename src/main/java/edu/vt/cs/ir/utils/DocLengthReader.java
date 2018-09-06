package edu.vt.cs.ir.utils;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.*;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;

/**
 * A utility class for efficiently storing and retriving document/field length.
 * Use the main Function to dump document/field length information to a local file.
 *
 * @author Jiepu Jiang (jiepu@cs.vt.edu)
 * @version 2018-09-04
 */
public class DocLengthReader {

    public static final String DEFAULT_PREFIX = "dl.";
    public static final int DEFAULT_BUFFER_SIZE = 1024 * 1024; // you can adjust the buffer size according to your RAM size

    protected FileChannel fch;
    protected IntBuffer buffer;

    protected int start;
    protected int bufferSize;

    protected double avg_dl;

    private static File getDocLengthFile( File dirIndex, String field ) {
        return new File( dirIndex, DEFAULT_PREFIX + field );
    }

    /**
     * @param dirIndex Path of the index directory.
     * @param field    Name of the index field.
     * @throws IOException
     */
    public DocLengthReader( File dirIndex, String field ) throws IOException {
        this( dirIndex, field, DEFAULT_BUFFER_SIZE );
    }

    /**
     * @param dirIndex   Path of the index directory.
     * @param field      Name of the index field.
     * @param bufferSize Buffer size for reading document length index.
     * @throws IOException
     */
    public DocLengthReader( File dirIndex, String field, int bufferSize ) throws IOException {
        File f = getDocLengthFile( dirIndex, field );
        DataInputStream dis = new DataInputStream( new FileInputStream( f ) );
        long skip = 0;
        while ( skip < f.length() - 8 ) {
            skip += dis.skip( f.length() - skip - 8 );
        }
        avg_dl = dis.readDouble();
        dis.close();
        Path path = f.toPath();
        this.fch = FileChannel.open( path );
        this.start = 0;
        this.bufferSize = bufferSize;
        mapBuffer();
    }

    private void mapBuffer() throws IOException {
        long size = bufferSize * 4;
        if ( fch.size() < start * 4 + size ) {
            size = fch.size() - start * 4;
        }
        this.buffer = fch.map( FileChannel.MapMode.READ_ONLY, start * 4, size ).asIntBuffer();
    }

    /**
     * @param docid An index document id.
     * @return The length of the field.
     * @throws IOException
     */
    public int getLength( int docid ) throws IOException {
        if ( docid - start >= bufferSize || docid < start ) {
            start = docid;
            mapBuffer();
        }
        return this.buffer.get( docid - start );
    }

    public void close() throws IOException {
        fch.close();
    }

    /**
     * @return The average length of the document field.
     */
    public double averageLength() {
        return avg_dl;
    }

    public static void main( String[] args ) {
        try {

            args = new String[]{
                    "C:\\Users\\Jiepu\\Downloads\\index_lucene_robust04_krovetz",
                    "content"
            };

            String pathIndex = args[ 0 ]; // path of the index
            String[] fields = args[ 1 ].split( ";" ); // name of a list of index fields, separated by ;

            File dirIndex = new File( pathIndex );

            Directory dir = FSDirectory.open( dirIndex.toPath() );
            IndexReader index = DirectoryReader.open( dir );

            for ( String field : fields ) {
                File f = DocLengthReader.getDocLengthFile( dirIndex, field );
                try {
                    System.out.println( " >> Dumping document length for field " + field );
                    double avdl = 0;
                    double count = 0;
                    DataOutputStream dos = new DataOutputStream( new BufferedOutputStream( new FileOutputStream( f ), 16 * 1024 * 1024 ) );
                    for ( int docid = 0; docid < index.numDocs(); docid++ ) {
                        if ( docid > 0 && docid % 1000000 == 0 ) {
                            System.out.println( "   --> finished " + docid + " documents" );
                        }
                        Terms terms = index.getTermVector( docid, field );
                        TermsEnum iterator = terms.iterator();
                        int doclen = 0;
                        while ( iterator.next() != null ) {
                            doclen += (int) iterator.totalTermFreq();
                        }
                        dos.writeInt( doclen );
                        avdl += doclen;
                        count++;
                    }
                    dos.writeDouble( avdl / count );
                    dos.close();
                } catch ( Exception e ) {
                    e.printStackTrace();
                }
            }

            index.close();
            dir.close();

        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

}
