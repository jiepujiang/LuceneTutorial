
## VT CS 5604 Information Retrieval (Fall 2018)
## A Simple Tutorial of Lucene (for CS 5604 Students)

_Last Update: Aug 21, 2018_

This tutorial is modified from another tutorial made for the CS 646 course at UMass Amherst in Fall 2016.

A quick index:
* [Installation](https://github.com/jiepujiang/LuceneExamples#installation)
* [Building an Index](https://github.com/jiepujiang/LuceneExamples#build-an-index)
* [Working with an Index](https://github.com/jiepujiang/LuceneExamples#working-with-an-index)
    * [External and Internal IDs](https://github.com/jiepujiang/LuceneExamples#external-and-internal-ids)
    * [Frequency Posting List](https://github.com/jiepujiang/LuceneExamples#frequency-posting-list)
    * [Position Posting List](https://github.com/jiepujiang/LuceneExamples#position-posting-list)
    * [Accessing an Indexed Document](https://github.com/jiepujiang/LuceneExamples#accessing-an-indexed-document)
    * [Document and Field Length](https://github.com/jiepujiang/LuceneExamples#document-and-field-length)
    * [Corpus-level Statistics](https://github.com/jiepujiang/LuceneExamples#corpus-level-statistics)
* [Searching](https://github.com/jiepujiang/LuceneExamples#searching)

## Environment

This tutorial uses:
* Oracle JDK 1.8
* Lucene 7.4

## Installation

Apache Lucene is an open-source information retrieval toolkit written in Java.

The easiest way to use Lucene in your project is to import it using Maven.
You need to at least import ```lucene-core``` (just pasting the following to your ```pom.xml```'s dependencies).

```xml
<dependency>
    <groupId>org.apache.lucene</groupId>
    <artifactId>lucene-core</artifactId>
    <version>7.4.0</version>
</dependency>
```

You may also need ```lucene-analyzers-common``` and ```lucene-queryparser``` as well.

```xml
<dependency>
    <groupId>org.apache.lucene</groupId>
    <artifactId>lucene-analyzers-common</artifactId>
    <version>7.4.0</version>
</dependency>
<dependency>
    <groupId>org.apache.lucene</groupId>
    <artifactId>lucene-queryparser</artifactId>
    <version>7.4.0</version>
</dependency>
```

If you do not use Maven, you need to download the jar files by yourself and include them into your project.
Make sure you download the correct version.
http://archive.apache.org/dist/lucene/java/7.4.0/

Support:
* Official API documentation: http://lucene.apache.org/core/7_4_0/

## Build an Index

### Corpus

This tutorial uses a small trectext format corpus (trectext is a popular format for data collections in information retrieval research).
You can download the corpus at https://github.com/jiepujiang/lucene_examples/blob/master/example_corpus.gz

The corpus includes the information of about 100 articles published in the SIGIR conferences.
Each article (document) is in the following format:

```xml
<DOC>
<DOCNO>ACM-383972</DOCNO>
<TITLE>Relevance based language models</TITLE>
<AUTHOR>Victor Lavrenko, W. Bruce Croft</AUTHOR>
<SOURCE>Proceedings of the 24th annual international ACM SIGIR conference on Research and development in information retrieval</SOURCE>
<TEXT>
We explore the relation between classical probabilistic models of information retrieval and the emerging language modeling approaches. It has long been recognized that the primary obstacle to effective performance of classical models is the need to estimate a relevance model : probabilities of words in the relevant class. We propose a novel technique for estimating these probabilities using the query alone. We demonstrate that our technique can produce highly accurate relevance models, addressing important notions of synonymy and polysemy. Our experiments show relevance models outperforming baseline language modeling systems on TREC retrieval and TDT tracking tasks. The main contribution of this work is an effective formal method for estimating a relevance model with no training data.
</TEXT>
</DOC>
```

A document has five fields.
The DOCNO field specifies a unique ID (docno) for each article.
We need to build an index for the other four text fields such that we can retrieve the documents.

### Text Processing and Indexing Options

Many IR systems may require you to specify a few text processing options for indexing and retrieval:
* **Tokenization** -- how to split a sequence of text into individual tokens (most tokens are just words).
The default tokenization option is usually enough for normal English-language text retrieval applications.
* **Letter case** -- Most IR systems ignore letter case differences between tokens/words. 
But sometimes letter case may be important, e.g., **smart** and **SMART** (the SMART retrieval system). 
* **Stop words** -- You may want to remove some stop words such as **is**, **the**, and **to**. 
Removing stop words can significantly reduce index size. 
But it may also cause problems for some search queries such as ```to be or not to be```.
We recommend you to keep them unless you cannot afford the disk space (quite cheap these days).
* **Stemming** -- Stemmers generate [word stems](https://en.wikipedia.org/wiki/Word_stem). You may want to index stemmed words rather than the original ones to ignore minor word differences such as **model** vs. **models**.
Stemming algorithms are not perfect and may get wrong. IR systems often use **Porter**, **Porter2**, or **Krovetz** stemming. Just a few examples for their differences:

Original    | Porter2   | Krovetz
--------    | -------   | -------
relevance   | relev     | relevance
based       | base      | base
language    | languag   | language
models      | model     | model

An indexed document can have different fields to store different types of information. 
Most IR systems support two types of fields:
* **Metadata field** is similar to a structured database record's field. 
They are stored and indexed as a whole without tokenization.
It is suitable for data such as IDs (such as the docno field in our example corpus).
Some IR systems support storing and indexing numeric values 
(and you can search for indexed numeric values using range or greater-than/less-than queries).
* **Normal text field** is suitable for regular text contents (such as the other four fields in our example corpus).
The texts are tokenized and indexed (using inverted index), such that you can search using normal text retrieval techniques.

### Lucene Examples

This is an example program that uses Lucene to build an index for the example corpus. 
```java
// change the following input and output paths to your local ones
String pathCorpus = "/Users/jiepu/Downloads/example_corpus.gz";
String pathIndex = "/Users/jiepu/Downloads/example_index_lucene";

Directory dir = FSDirectory.open( new File( pathIndex ).toPath() );

// Analyzer specifies options for text processing
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

IndexWriterConfig config = new IndexWriterConfig( analyzer );
// Note that IndexWriterConfig.OpenMode.CREATE will override the original index in the folder
config.setOpenMode( IndexWriterConfig.OpenMode.CREATE );

IndexWriter ixwriter = new IndexWriter( dir, config );

// This is the field setting for metadata field.
FieldType fieldTypeMetadata = new FieldType();
fieldTypeMetadata.setOmitNorms( true );
fieldTypeMetadata.setIndexOptions( IndexOptions.DOCS );
fieldTypeMetadata.setStored( true );
fieldTypeMetadata.setTokenized( false );
fieldTypeMetadata.freeze();

// This is the field setting for normal text field.
FieldType fieldTypeText = new FieldType();
fieldTypeText.setIndexOptions( IndexOptions.DOCS_AND_FREQS_AND_POSITIONS );
fieldTypeText.setStoreTermVectors( true );
fieldTypeText.setStoreTermVectorPositions( true );
fieldTypeText.setTokenized( true );
fieldTypeText.setStored( true );
fieldTypeText.freeze();

// You need to iteratively read each document from the corpus file,
// create a Document object for the parsed document, and add that
// Document object by calling addDocument().

// Well, the following only works for small text files. DO NOT follow this part in your homework!
InputStream instream = new GZIPInputStream( new FileInputStream( pathCorpus ) );
String corpusText = new String( IOUtils.toByteArray( instream ), "UTF-8" );
instream.close();

Pattern pattern = Pattern.compile(
        "<DOC>.+?<DOCNO>(.+?)</DOCNO>.+?<TITLE>(.+?)</TITLE>.+?<AUTHOR>(.+?)</AUTHOR>.+?<SOURCE>(.+?)</SOURCE>.+?<TEXT>(.+?)</TEXT>.+?</DOC>",
        Pattern.CASE_INSENSITIVE + Pattern.MULTILINE + Pattern.DOTALL
);

Matcher matcher = pattern.matcher( corpusText );

while ( matcher.find() ) {

    String docno = matcher.group( 1 ).trim();
    String title = matcher.group( 2 ).trim();
    String author = matcher.group( 3 ).trim();
    String source = matcher.group( 4 ).trim();
    String text = matcher.group( 5 ).trim();

    // Create a Document object
    Document d = new Document();
    // Add each field to the document with the appropriate field type options
    d.add( new Field( "docno", docno, fieldTypeMetadata ) );
    d.add( new Field( "title", title, fieldTypeText ) );
    d.add( new Field( "author", author, fieldTypeText ) );
    d.add( new Field( "source", source, fieldTypeText ) );
    d.add( new Field( "text", text, fieldTypeText ) );
    // Add the document to index.
    ixwriter.addDocument( d );
}

// remember to close both the index writer and the directory
ixwriter.close();
dir.close();
```

You can download the Lucene index for the example corpus at https://github.com/jiepujiang/LuceneExamples/blob/master/example_index_lucene.tar.gz

## Working with an Index

### Openning and Closing an Index

Lucene uses the IndexReader class to operate all index files.

```java
// modify to your index path
String pathIndex = "index_example_lucene"; 

// First, open the directory
Directory dir = FSDirectory.open( new File( pathIndex ).toPath() );

// Then, open an IndexReader to access your index
IndexReader index = DirectoryReader.open( dir );

// Now, start working with your index using the IndexReader object

ixreader.numDocs(); // just an example; get the number of documents in the index

// Remember to close both the IndexReader and the Directory after use 
index.close();
dir.close();
``` 

### External and Internal IDs

In IR experiments, we often use some unique IDs to identify documents in the corpus. 
For example, our example corpus (and most TREC corpora) uses docno as the unique identifer.

However, IR systems often use some internal IDs to identify the indexed documents.
These IDs are often subject to change and not transparent to the users.
So you often need to transform between external and internal IDs when locating documents in an index.

To help you get started quickly, we provide
a utility class ```edu.vt.cs.ir.lucene.LuceneUtils``` to help you transform between 
the two IDs.
```java
IndexReader index = DirectoryReader.open( dir );

// the name of the field storing external IDs (docnos)
String fieldName = "docno";

int docid = 5;
LuceneUtils.getDocno( index, fieldName, docid ); // get the docno for the internal docid = 5

String docno = "ACM-1835461";
LuceneUtils.findByDocno( index, fieldName, docno ); // get the internal docid for docno "ACM-1835461"
```

### Frequency Posting List

You can retrieve a term's posting list from an index.
The simplest form is document-frequency posting list,
where each entry in the list is a ```<docid,frequency>``` pair (only includes the documents containing that term).
The entries are sorted by docids such that you can efficiently compare and merge multiple lists.

The following program retrieves the posting list for a term ```reformulation``` in the ```text``` field from the Lucene index:
```java
String pathIndex = "/home/jiepu/Downloads/example_index_lucene";
			
// Let's just retrieve the posting list for the term "reformulation" in the "text" field
String field = "text";
String term = "reformulation";

Directory dir = FSDirectory.open( new File( pathIndex ).toPath() );
IndexReader index = DirectoryReader.open( dir );

// The following line reads the posting list of the term in a specific index field.
// You need to encode the term into a BytesRef object,
// which is the internal representation of a term used by Lucene.
System.out.printf( "%-10s%-15s%-6s\n", "DOCID", "DOCNO", "FREQ" );
PostingsEnum posting = MultiFields.getTermDocsEnum( index, field, new BytesRef( term ), PostingsEnum.FREQS );
if ( posting != null ) { // if the term does not appear in any document, the posting object may be null
    int docid;
    // Each time you call posting.nextDoc(), it moves the cursor of the posting list to the next position
    // and returns the docid of the current entry (document). Note that this is an internal Lucene docid.
    // It returns PostingsEnum.NO_MORE_DOCS if you have reached the end of the posting list.
    while ( ( docid = posting.nextDoc() ) != PostingsEnum.NO_MORE_DOCS ) {
        String docno = LuceneUtils.getDocno( index, "docno", docid );
        int freq = posting.freq(); // get the frequency of the term in the current document
        System.out.printf( "%-10d%-15s%-6d\n", docid, docno, freq );
    }
}

index.close();
dir.close();
```

The output is:
```
DOCID     DOCNO          FREQ  
3         ACM-2010085    1     
10        ACM-1835626    2     
24        ACM-1277796    1     
94        ACM-2484096    1     
98        ACM-2348355    4     
104       ACM-2609633    1     
```

Note that the internal docids are subject to change and 
are often different between different systems and indexes. 

### Position Posting List

You can also retrieve a posting list with term postions in each document.

```java
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
PostingsEnum posting = MultiFields.getTermDocsEnum( index, field, new BytesRef( term ), PostingsEnum.POSITIONS );
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
```

The output is:
```
DOCID     DOCNO          FREQ      POSITIONS           
3         ACM-2010085    1         56
10        ACM-1835626    2         1,73
24        ACM-1277796    1         157
94        ACM-2484096    1         12
98        ACM-2348355    4         84,117,156,177
104       ACM-2609633    1         153
```

### Accessing An Indexed Document

You can access an indexed document from an index. 
An index document is usually stored as a document vector, 
which is a list of <word,frequency> pairs.

```java
String pathIndex = "/home/jiepu/Downloads/example_index_lucene";

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
```

```
TERM                FREQ      POSITIONS           
1,800               1         92
2007                1         79
95                  1         148
a                   2         19,119
acquire             1         86
algorithms          1         84
along               1         100
analysis            1         103
and                 3         42,111,132
appreciable         1         151
are                 1         51
as                  2         133,135
assessor            1         142
at                  1         77
available           1         52
be                  2         59,145
been                2         5,18
best                1         36
between             1         106
by                  1         147
can                 1         144
completeness        1         15
cost                1         130
deal                1         21
deeper              1         102
document            1         82
documents           1         39
dozen               1         9
each                1         11
effective           1         131
effort              2         72,143
errors              1         155
estimate            1         45
evaluate            1         62
evaluation          5         2,26,46,121,154
few                 1         49
fewer               2         126,136
for                 1         89
great               1         20
has                 2         3,17
how                 2         32,43
in                  2         53,153
increase            1         152
information         1         0
investigating       1         104
is                  1         128
it                  1         57
judge               1         41
judged              1         12
judging             1         71
judgment            1         30
judgments           5         50,88,114,127,140
light               1         54
many                1         64
measures            1         47
million             1         74
more                6         65,69,90,123,129,139
much                2         28,68
near                1         14
no                  1         150
number              2         108,112
of                  6         22,38,55,97,109,113
on                  1         25
over                4         7,27,63,122
performed           1         6
point               1         120
possible            1         60
present             1         95
queries             6         10,66,93,110,124,137
query               1         75
recent              1         23
reduced             1         146
relevance           1         87
reliable            1         134
results             1         96
retrieval           1         1
select              1         34
selection           1         83
set                 1         37
sets                1         31
several             1         8
should              1         58
shows               1         115
smaller             1         29
than                1         91
that                1         116
the                 4         35,73,98,107
there               1         16
this                1         56
to                  7         13,33,40,44,61,85,118
total               2         70,141
track               2         76,99
tradeoffs           1         105
trec                1         78
two                 1         81
typically           1         4
up                  1         117
used                1         80
we                  1         94
when                1         48
with                4         101,125,138,149
without             1         67
work                1         24
```

There are some slight differences between the results by Lucene and Galago.
They are caused by the differences of the two systems in default tokenization. 
For example, Lucene will not split 1,800 into two tokens but Galado will.

### Document and Field Length

Unfortunately, by the time I created the tutorial, Lucene does not store document length (the total word count) in its index. 
An acceptable but slow solution is that you calculate document length by yourself based on a document
vector. In case your dataset is static and relatively small (such as just about or less than a few
million documents), you can simply compute all documents' lengths after you've built an index and store
them in an external file (it takes just 4MB to store 1 million docs' lengths as integers). At running
time, you can load all the computed document lengths into memory to avoid loading doc vector and computing length again.

The following program prints out the length of text field for each document in the example corpus, 
which also helps you understand how to work with a stored document vector:
```java
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
```

The output is:
```
DOCID     DOCNO          Length    
0         ACM-2009969    187       
1         ACM-2009998    151       
2         ACM-2010026    136       
3         ACM-2010085    117       
4         ACM-1835458    142       
5         ACM-1835461    132       
6         ACM-1835493    175       
7         ACM-1835499    171       
8         ACM-1835521    156       
9         ACM-1835602    92        
10        ACM-1835626    112       
11        ACM-1835637    67        
12        ACM-1835650    110       
13        ACM-1572050    130       
14        ACM-1572139    99        
15        ACM-1572140    83        
16        ACM-1390339    187       
17        ACM-1390376    165       
18        ACM-1390416    117       
19        ACM-1390419    134       
20        ACM-1390432    110       
21        ACM-1390445    156       
22        ACM-1277758    168       
23        ACM-1277774    91        
24        ACM-1277796    158       
25        ACM-1277835    170       
26        ACM-1277868    69        
27        ACM-1277920    59        
28        ACM-1277922    155       
29        ACM-1277947    107       
30        ACM-1148204    140       
31        ACM-1148212    99        
32        ACM-1148219    246       
33        ACM-1148250    136       
34        ACM-1148305    102       
35        ACM-1148310    5         
36        ACM-1148324    104       
37        ACM-1076074    159       
38        ACM-1076109    167       
39        ACM-1076115    122       
40        ACM-1076156    100       
41        ACM-1076190    5         
42        ACM-1009026    96        
43        ACM-1009044    102       
44        ACM-1009098    86        
45        ACM-1009110    5         
46        ACM-1009114    5         
47        ACM-1008996    149       
48        ACM-860437     1253      
49        ACM-860479     167       
50        ACM-860493     107       
51        ACM-860548     5         
52        ACM-860549     37        
53        ACM-564394     95        
54        ACM-564408     152       
55        ACM-564429     151       
56        ACM-564430     78        
57        ACM-564441     5         
58        ACM-564465     58        
59        ACM-383954     105       
60        ACM-383972     115       
61        ACM-384022     120       
62        ACM-345674     5         
63        ACM-345546     137       
64        ACM-312679     5         
65        ACM-312687     5         
66        ACM-312698     5         
67        ACM-290954     5         
68        ACM-290958     5         
69        ACM-290987     5         
70        ACM-291008     5         
71        ACM-291043     5         
72        ACM-258540     5         
73        ACM-258547     5         
74        ACM-243202     5         
75        ACM-243274     5         
76        ACM-243276     5         
77        ACM-215328     5         
78        ACM-215380     5         
79        ACM-188586     5         
80        ACM-160728     85        
81        ACM-160760     5         
82        ACM-160761     76        
83        ACM-160689     81        
84        ACM-133203     145       
85        ACM-122864     5         
86        ACM-636811     81        
87        ACM-511797     5         
88        ACM-636717     93        
89        ACM-511760     195       
90        ACM-511717     124       
91        ACM-803136     129       
92        ACM-2484097    206       
93        ACM-2484069    249       
94        ACM-2484096    191       
95        ACM-2484060    278       
96        ACM-2484139    157       
97        ACM-2348296    157       
98        ACM-2348355    189       
99        ACM-2348408    157       
100       ACM-2348426    80        
101       ACM-2348440    5         
102       ACM-2609628    170       
103       ACM-2609467    136       
104       ACM-2609633    245       
105       ACM-2609503    126       
106       ACM-2609485    109       
107       ACM-2609536    194       
```

### Corpus-level Statistics

```IndexReader``` provides many corpus-level statistics.
The follow program computes the IDF and corpus probability for the term ```reformulation```.
```java
String pathIndex = "/home/jiepu/Downloads/example_index_lucene";
			
// Let's just count the IDF and P(w|corpus) for the word "reformulation" in the "text" field
String field = "text";
String term = "reformulation";

Directory dir = FSDirectory.open( new File( pathIndex ).toPath() );
IndexReader index = DirectoryReader.open( dir );

int N = index.numDocs(); // the total number of documents in the index
int n = index.docFreq( new Term( field, term ) ); // get the document frequency of the term in the "text" field
double idf = Math.log( ( N + 1 ) / ( n + 1 ) ); // well, we normalize N and n by adding 1 to avoid n = 0

System.out.printf( "%-30sN=%-10dn=%-10dIDF=%-8.2f\n", term, N, n, idf );

long corpusTF = index.totalTermFreq( new Term( field, term ) ); // get the total frequency of the term in the "text" field
long corpusLength = index.getSumTotalTermFreq( field ); // get the total length of the "text" field
double pwc = 1.0 * corpusTF / corpusLength;

System.out.printf( "%-30slen(corpus)=%-10dfreq(%s)=%-10dP(%s|corpus)=%-10.6f\n", term, corpusLength, term, corpusTF, term, pwc );

// remember to close the index and the directory
index.close();
dir.close();
```

The output is:
```
reformulation                 N=108       n=6         IDF=2.71    
reformulation                 len(corpus)=12077     freq(reformulation)=10        P(reformulation|corpus)=0.000828
```

## Searching

We will not use Lucene's own search functionalities much in this course.
But just to help you get started with, the following program retrieves the top 10 
articles for the query "query reformulation" from the example corpus using a variant 
of the BM25 retrieval model (implemented in Lucene).

```java
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
```

The output is:
```
Rank      DocNo               Score     Title
1         ACM-2348355         5.7742    Generating reformulation trees for complex queries
2         ACM-1835626         5.2476    Learning to rank query reformulations
3         ACM-2010085         4.3674    Modeling subset distributions for verbose queries
4         ACM-1277796         3.8501    Latent concept expansion using markov random fields
5         ACM-2484096         3.7127    Compact query term selection using topically related text
6         ACM-2609633         2.8809    Searching, browsing, and clicking in a search session: changes in user behavior by task and over time
7         ACM-1835637         1.6526    Query term ranking based on dependency parsing of verbose queries
8         ACM-2348408         1.6438    Modeling higher-order term dependencies in information retrieval using query hypergraphs
9         ACM-2609467         1.6367    Diversifying query suggestions based on query documents
10        ACM-1572140         1.6168    Two-stage query segmentation for information retrieval
```