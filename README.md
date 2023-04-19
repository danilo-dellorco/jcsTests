# ISW2 Tests for JCS
Unit and integration tests made for JCS Software. 

## Testing Methodology
JCS (Java Caching System) è un sistema di cache distribuito scritto in Java. Lo scopo di questa attività è
quello di scrivere dei test parametrici, andando a convertire da JUnit3 a JUnit4 i test originali di JCS.
Seguendo l’algoritmo di scelta, i test considerati sono JCSRemovalSimpleConcurrentTest.java e
JCSThrashTest.java.
Per convertire i test da JUnit3 a JUnit4 si è cercato di mantenere inalterata l’implementazione dei test case,
andando semplicemente ad annotare con @Test i metodi utilizzati nella Test Suite originale. Per rendere il
test parametrico è stato quindi utilizzato il runner Parametrized.class, ed è stato definito un metodo
data() con annotazione @Parameters per specificare i parametri da passare al costruttore della classe di
test. Successivamente sono stati ricavati i parametri utilizzati nei vari casi di test, per poi andare ad inserirli
nel metodo data() ed eseguire diversi test case in base ai diversi parametri considerati. Con un approccio
di questo tipo risulta molto più semplice mantenere ed ampliare la test suite, in quanto per descrivere un
nuovo caso di test basterà aggiungere una nuova entry nell’array ritornato dal metodo data().
Per effettuare il setup dell’ambiente è stato definito il metodo configure()con annotazione
@BeforeClass. Questo metodo, che viene eseguito una sola volta prima dell’esecuzione dei casi di test, si
occupa di ottenere un’istanza di JCS e di impostare il corretto file .ccf di configurazione.
In linea generale per riscrivere i nuovi Test Case si è cercato di utilizzare un approccio di tipo black-box,
basandosi sui commenti presenti nel codice e nei casi di test originali.

## JCSREMOVALSIMPLECONCURRENTTEST.JAVA
L'obiettivo di questa classe è quello di testare che la pulizia della cache e la rimozione di entry vengano
eseguite correttamente. In particolare si effettua il put()per inserire una o più entry in cache, e poi si
esegue remove() o clear() per verificare che l’entry inserita sia stata rimossa correttamente.
Durante la progettazione del test parametrico è stato identificato come unico valore variabile l’intero count.
Questo parametro definisce il numero di operazioni put() che verranno effettuate, e quindi il numero di
entry che saranno prima inserite nella cache, e poi rimosse tramite remove() o clear(). Nei test originali
di JCS il valore di count è sempre impostato a 500, quindi per una prima valutazione della copertura
strutturale è stato utilizzato soltanto questo valore come input.
Altri possibili parametri utilizzati in JCS sono la coppia chiave/valore relativa all’entry che verrà inserita in
cache. Tuttavia non è sembrato di particolare interesse far variare questi valori in quanto nel test originale
viene semplicemente usata una stringa “key” ed una stringa “value” per ogni operazione di put().

## JCSTHRASHTEST.JAVA
L’obiettivo di questa classe è quello di testare l’inserimento di una entry, la rimozione, e la pulizia della
cache, tramite i metodi put(), remove() e clear().Inoltre viene anche valutato il comportamento della
memoria dopo aver eseguito queste operazioni.
I valori individuati per la progettazione del test parametrico sono il numero di thread che vengono spawnati
ed il numero di chiavi che vengono inserite nella cache. Questi valori nel test originale non sono definiti
come variabili ma semplicemente come interi che vanno a definire l’end condition del ciclo for; di default
vengono generati 15 thread ed inserite 500 chiavi. Sono state quindi rimossi questi valori hard-coded e al
loro posto sono state inserite delle variabili di classe numThreads e numKeys. Per una prima valutazione
sul coverage del test originale sono stati utilizzati soltanto i valori di default, ponendo quindi
numThreads=15 e numKeys=500.
Altre variabili utilizzate nel test originale sono le stringhe value/key che sono semplicemente inizializzate a
"value" e "key". Tuttavia analizzando l’implementazione originale del test si è visto che non c’è nessuna
asserzione o nessun controllo su eventuali eccezioni lanciate, per cui si è ritenuto non rilevante andare a
parametrizzare questi valori testando ad esempio stringhe nulle o vuote. Infatti per ampliare i comportamenti
catturati da questo test case sarebbe necessario modificarne l’implementazione, ma dovendo soltanto
convertire il test si è ritenuto non conforme allo scopo di questa attività.

## COVERAGE
Per analizzare il coverage ottenuto tramite le due classi di test è stato definito all’interno del pom.xml un
profilo di coverage. Impostando questo profilo durante il build del progetto viene lanciato il plugin
jacoco:prepare-agent. Non avendo il codice sorgente di JCS, Jacoco non può fornire nessun report sul
coverage, per cui è necessario fornire al plugin una versione instrumented di jcs.jar. Al termine
dell’esecuzione del goal prepare-agent, Jacoco genera un report di tipo.exec; al fine di analizzare tale
report e valutare la copertura raggiunta, è necessario convertirlo in un formato human-readable. Per fare ciò
è stato incluso nel profilo di coverage il plugin exec-maven-plugin, che si occupa di lanciare durante la
fase di maven:verify uno script bash che converte il report exec in diversi formati, tra cui html.
Durante questa fase di analisi è stato utilizzato un approccio di tipo black-box, andando ad osservare il
codice nello specifico in modo tale da motivare i risultati ottenuti dal report sul coverage. La classe stimolata
dai due casi di test considerati è org.apache.jcs.access.CacheAccess.java ed in particolare
vengono sollecitati i metodi get(), put(), remove() e clear().
I risultati del coverage [Figura 1] mostrano che per i metodi get(), put() e remove() è stato raggiunto il
100% di coverage. Ciò vuol dire che i casi di test originali di JCS vanno già a testare tutte le istruzioni
presenti nelle implementazioni di questi metodi.
Per quanto riguarda il metodo clear() è stata raggiunta invece una coverage del 45%. Analizzando il
codice di questo metodo si è notato che il motivo per cui non si raggiunge il 100% di coverage è dovuto al
fatto che l’implementazione dei Test JCS non prevede nemmeno un caso di test che mandi in errore la
funzione removeAll() e che vada di conseguenza a stimolare il blocco catch lanciando l’eccezione
CacheException.
public void clear() throws CacheException {
try {
 this.cacheControl.removeAll();
 } catch ( IOException e ) {
 throw new CacheException( e );
 }
Per cercare di migliorare il coverage è stata quindi estesa la test suite; applicando boundary value analysis
sono stati considerati come ulteriori valori anche 0, 1 e -1 per count, numKeys e numThreads. Tuttavia i
risultati del nuovo report hanno mostrato una coverage per il metodo clear()ancora pari al 45%.

E’ stato dunque effettuato un ulteriore tentativo per sviluppare un caso di test che vada a lanciare
l’eccezione nel metodo clear(), andando a vedere direttamente l’implementazione del metodo
removeAll(). Questo metodo invoca CompositeCache.removeAll(), ma anche analizzandone il
codice non si è riusciti a trovare un possibile valore che permettesse di lanciare l’eccezione.
In conclusione, si è ritenuto che seguendo gli obiettivi ed i requisiti di questa attività, la copertura dei casi di
test non fosse ulteriormente migliorabile, in quanto per migliorare il coverage del metodo clear()non basta
ampliare la test suite, ma è necessario modificare e migliorare l’implementazione dei casi di test considerati.
