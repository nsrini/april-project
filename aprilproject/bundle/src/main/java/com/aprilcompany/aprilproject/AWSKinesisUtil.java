package com.aprilcompany.aprilproject;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.kinesis.producer.Attempt;
import com.amazonaws.services.kinesis.producer.KinesisProducer;
import com.amazonaws.services.kinesis.producer.KinesisProducerConfiguration;
import com.amazonaws.services.kinesis.producer.UserRecordFailedException;
import com.amazonaws.services.kinesis.producer.UserRecordResult;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.apache.commons.lang.math.*;
import com.day.cq.security.*;
public class AWSKinesisUtil {
    public static final String REGION = "us-west-2";
    public static final String STREAM_NAME = "march-stream-three";
    private static final Logger log = LoggerFactory.getLogger(AWSKinesisUtil.class.getName());

	public static void sendToKinesisStream(final String json) { 
        final KinesisProducer producer = getKinesisProducer();
        
     // KinesisProducer.addUserRecord is asynchronous. A callback can be used to receive the results.
        final FutureCallback<UserRecordResult> callback = new FutureCallback<UserRecordResult>() {
            @Override
            public void onFailure(Throwable t) {
            	log.debug("112");

                // We don't expect any failures during this sample. If it
                // happens, we will log the first one and exit.
                if (t instanceof UserRecordFailedException) {
                    Attempt last = Iterables.getLast(
                            ((UserRecordFailedException) t).getResult().getAttempts());
                    log.error(String.format(
                            "Record failed to put - %s : %s",
                            last.getErrorCode(), last.getErrorMessage()));
                }
                log.error("Exception during put", t);
                System.exit(1);
            }

			@Override
			public void onSuccess(UserRecordResult arg0) {
				log.debug("success");
				
			}
        };
        
        // The lines within run() are the essence of the KPL API.
        final Runnable putOneRecord = new Runnable() {
            @Override
            public void run() {
                // TIMESTAMP is our partition key
            	String timeStamp = new Timestamp(new Date().getTime()).toString();
            	log.debug("11");
                try {
					ListenableFuture<UserRecordResult> f =
					        producer.addUserRecord(STREAM_NAME, timeStamp, ByteBuffer.wrap(json.getBytes("UTF-8")));
	                Futures.addCallback(f, callback);

				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
        };
        putOneRecord.run();
        producer.flushSync();
        producer.destroy();
        log.info("Finished.");
	}
    public static KinesisProducer getKinesisProducer() {
        // There are many configurable parameters in the KPL. See the javadocs
        // on each each set method for details.
        KinesisProducerConfiguration config = new KinesisProducerConfiguration();
        
        // You can also load config from file. A sample properties file is
        // included in the project folder.
        // KinesisProducerConfiguration config =
        //     KinesisProducerConfiguration.fromPropertiesFile("default_config.properties");
        
        // If you're running in EC2 and want to use the same Kinesis region as
        // the one your instance is in, you can simply leave out the region
        // configuration; the KPL will retrieve it from EC2 metadata.
        config.setRegion(REGION);
        
        // You can pass credentials programmatically through the configuration,
        // similar to the AWS SDK. DefaultAWSCredentialsProviderChain is used
        // by default, so this configuration can be omitted if that is all
        // that is needed.
        config.setCredentialsProvider(new DefaultAWSCredentialsProviderChain());
        
        // The maxConnections parameter can be used to control the degree of
        // parallelism when making HTTP requests. We're going to use only 1 here
        // since our throughput is fairly low. Using a high number will cause a
        // bunch of broken pipe errors to show up in the logs. This is due to
        // idle connections being closed by the server. Setting this value too
        // large may also cause request timeouts if you do not have enough
        // bandwidth.
        config.setMaxConnections(1);
        
        // Set a more generous timeout in case we're on a slow connection.
        config.setRequestTimeout(60000);
        
        // RecordMaxBufferedTime controls how long records are allowed to wait
        // in the KPL's buffers before being sent. Larger values increase
        // aggregation and reduces the number of Kinesis records put, which can
        // be helpful if you're getting throttled because of the records per
        // second limit on a shard. The default value is set very low to
        // minimize propagation delay, so we'll increase it here to get more
        // aggregation.
        config.setRecordMaxBufferedTime(15000);

        // If you have built the native binary yourself, you can point the Java
        // wrapper to it with the NativeExecutable option. If you want to pass
        // environment variables to the executable, you can either use a wrapper
        // shell script, or set them for the Java process, which will then pass
        // them on to the child process.
        // config.setNativeExecutable("my_directory/kinesis_producer");
        
        // If you end up using the default configuration (a Configuration instance
        // without any calls to set*), you can just leave the config argument
        // out.
        //
        // Note that if you do pass a Configuration instance, mutating that
        // instance after initializing KinesisProducer has no effect. We do not
        // support dynamic re-configuration at the moment.
        KinesisProducer producer = new KinesisProducer(config);
        
        return producer;
    }
}