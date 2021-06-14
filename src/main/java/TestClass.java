import org.apache.activemq.artemis.nativo.jlibaio.LibaioContext;
import org.apache.activemq.artemis.nativo.jlibaio.LibaioFile;
import org.apache.activemq.artemis.nativo.jlibaio.SubmitInfo;

import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

public class TestClass implements SubmitInfo {
    private static final int LIBAIO_QUEUE_SIZE = 50;
    static final AtomicLong position = new AtomicLong(0);
    final CountDownLatch countDownLatch = new CountDownLatch(LIBAIO_QUEUE_SIZE*10);

    public static void main(String[] args) throws Exception{
        TestClass testClass = new TestClass();
     //   testClass.aio();
        testClass.nio();
    }
    public void nio() throws Exception{
        FileOutputStream fileOutputStream = new FileOutputStream("/home/avinash/Music/projects/amq/activemq-artemis-native/src/main/resources/file_nio.txt",true);
        FileChannel inChannel = fileOutputStream.getChannel();
        byte bytes[] = new byte[10000*4096];
        for (int i = 0; i<bytes.length; i++){
            bytes[i] = ((byte) 'a');
        }
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        System.out.println("Before Write ");
        long time = System.currentTimeMillis();
        for(int i=0; i< 50; i++) {
            inChannel.position(i*bytes.length).write(buffer);
            inChannel.force(true);
        }
        System.out.println("Time Taken "+(System.currentTimeMillis()-time));
        inChannel.close();
    }
    public void aio() throws Exception{
        int NUMBER_OF_BLOCKS = LIBAIO_QUEUE_SIZE * 10;

        final LibaioContext<TestClass> testClassLibaioContext = new LibaioContext<>(LIBAIO_QUEUE_SIZE,true,true);
        Thread t = new Thread() {
            @Override
            public void run() {
                testClassLibaioContext.poll();
            }
        };

        t.start();
        int m_4kb = 10000;
        LibaioFile<TestClass> libaioFile = testClassLibaioContext.openFile("/home/avinash/Music/projects/amq/activemq-artemis-native/src/main/resources/file.txt",true);
        ByteBuffer buffer = LibaioContext.newAlignedBuffer(m_4kb*4096, 4096);

        for (int i = 0; i < m_4kb*4096; i++) {
            buffer.put((byte) 'a');
        }

        libaioFile.fill(libaioFile.getBlockSize(),LIBAIO_QUEUE_SIZE*m_4kb*4096);
        System.out.println("AIO Before Write ");
        long time = System.currentTimeMillis();
        for(int i=0;i<NUMBER_OF_BLOCKS; i++) {
            libaioFile.write(0, m_4kb * 4096, buffer, this);
        }
        System.out.println("AIO Time Taken "+(System.currentTimeMillis()-time));
       //libaioFile.read(0,uuid.length(),buffer2,new TestClass());
       /*byte[] bytes = new byte[2048];
       buffer2.get(bytes);
        System.out.println(new String(bytes));*/
        countDownLatch.await();
        testClassLibaioContext.close();
        t.join();
    }

    @Override
    public void onError(int errno, String message) {
        System.out.println(">> Error >>");
    }

    @Override
    public void done() {
        System.out.println(">>>>>>>>>>>> Finished >>> "+ System.currentTimeMillis());
        countDownLatch.countDown();
    }


   /* public static void test() throws Exception{

        CountDownLatch countDownLatch = new CountDownLatch(1);

*//*        Properties properties = new Properties();
        properties.setProperty(Context.INITIAL_CONTEXT_FACTORY,ActiveMQInitialContextFactory.class.getName());
        properties.setProperty(Context.PROVIDER_URL,"tcp://localhost:61616?minLargeMessageSize=2M");
        properties.setProperty(Context.SECURITY_PRINCIPAL,"admin");
        properties.setProperty(Context.SECURITY_CREDENTIALS,"admin");*//*
        String url = "tcp://localhost:61616";
        String cred = "adminc";
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(url,cred,cred);
        connectionFactory.setMinLargeMessageSize(10240000);
        byte[] bytes  = new byte[101376];

        Connection connection = connectionFactory.createConnection();
        connection.start();
        Session session  = connection.createSession(false,Session.AUTO_ACKNOWLEDGE);
        Queue queue = session.createQueue("queue1");
        MessageProducer messageProducer = session.createProducer(queue);
        BytesMessage bytesMessage = session.createBytesMessage();
        bytesMessage.setStringProperty("xyz","10");
        bytesMessage.writeBytes(bytes);
        messageProducer.send(bytesMessage);


        session.createConsumer(queue).setMessageListener(message -> {
            System.out.println(message instanceof BytesMessage);


        });
        System.out.println(">> Sent >> ");
        connection.close();

       countDownLatch.await();
    }
*/

}
