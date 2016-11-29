/*
 * Copyright 2013 Google Inc.
 * Copyright 2014 Andreas Schildbach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.bitcoinj.core.*;
import org.bitcoinj.crypto.KeyCrypterException;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.RegTestParams;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.utils.BriefLogFormatter;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.listeners.AbstractWalletEventListener;
import org.bitcoinj.wallet.listeners.WalletCoinsReceivedEventListener;
import org.bitcoinj.wallet.SendRequest;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * ForwardingService demonstrates basic usage of the library. It sits on the network and when it receives coins, simply
 * sends them onwards to an address given on the command line.
 */
public class BotMaster {
    private static Address botAddress;
    private static WalletAppKit kit;
    private static File walletFile;
    
    private static final int SHA256_LENGTH = 32;
	private static final int MAX_PREFIX_LENGTH = 8;
	private static final byte NULL_BYTE = (byte) '\0';

    public static void main(String[] args) throws Exception {
        // This line makes the log output more compact and easily read, especially when using the JDK log adapter.
        BriefLogFormatter.init();
        

        // Figure out which network we should connect to. Each one gets its own set of files.
        NetworkParameters params;
        String filePrefix;
        params = TestNet3Params.get();
        filePrefix = "forwarding-service-testnet";
        
        // Parse the address given as the first parameter.
        //Indirizzo del bot...inserire l'indirizzo corretto
        //botAddress = Address.fromBase58(params, "mkXJLEzLrqkEHAU6XjnxcpBdAsvhfAj5ow");
        botAddress = new Address(params, "mnPyV4WvegUdaq2KD7J2rdN8zZzgSC2ZkD");

        walletFile = new File("Master");
        
        // Start up a basic app using a class that automates some boilerplate.
        kit = new WalletAppKit(params, walletFile, filePrefix);


        // Download the block chain and wait until it's done.
        kit.startAsync();
        kit.awaitRunning();
        
        // We want to know when we receive money.
        kit.wallet().addCoinsReceivedEventListener(new WalletCoinsReceivedEventListener() {
            @Override
            public void onCoinsReceived(Wallet w, Transaction tx, Coin prevBalance, Coin newBalance) {
                // Runs in the dedicated "user thread" (see bitcoinj docs for more info on this).
                //
                // The transaction "tx" can either be pending, or included into a block (we didn't see the broadcast).
                Coin value = tx.getValueSentToMe(w);
                System.out.println("Received tx for " + value.toFriendlyString() + ": " + tx);
                //System.out.println("Transaction will be forwarded after it confirms.");
                // Wait until it's made it into the block chain (may run immediately if it's already there).
                //
                // For this dummy app of course, we could just forward the unconfirmed transaction. If it were
                // to be double spent, no harm done. Wallet.allowSpendingUnconfirmedTransactions() would have to
                // be called in onSetupCompleted() above. But we don't do that here to demonstrate the more common
                // case of waiting for a block. 
                
                Futures.addCallback(tx.getConfidence().getDepthFuture(1), new FutureCallback<TransactionConfidence>() {
                    @Override
                    public void onSuccess(TransactionConfidence result) {
                        System.out.println("Transazione ricevuta con successo!!!!");
                        System.out.println("Il Master ha: " +w.getBalance().toFriendlyString());
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        // This kind of future can't fail, just rethrow in case something weird happens.
                        throw new RuntimeException(t);
                    }
                });
            }
        });
        

        Address sendToAddress = kit.wallet().currentReceiveKey().toAddress(params);
        System.out.println("Indirizzo del Master: " + sendToAddress);
        System.out.println("Il Master ha: " +kit.wallet().getBalance().toFriendlyString());
        System.out.println("Waiting for coins to arrive. Press Ctrl-C to quit.");
        
        //send();
        sendCommand("Hello Bot!!!!");
        
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException ignored) {}
    }
    
    public static void send() throws InsufficientMoneyException {
    	System.out.println("Forwarding " + Coin.MILLICOIN + " BTC");
    	// Now send the coins back! Send with a small fee attached to ensure rapid confirmation.
    	final Wallet.SendResult sendResult = kit.wallet().sendCoins(kit.peerGroup(), botAddress, Coin.MILLICOIN);
    	System.out.println("Sending ...");
    	// Register a callback that is invoked when the transaction has propagated across the network.
    	// This shows a second style of registering ListenableFuture callbacks, it works when you don't
    	// need access to the object the future returns.
    	sendResult.broadcastComplete.addListener(new Runnable() {
    	    @Override
    	    public void run() {
    	         // The wallet has changed now, it'll get auto saved shortly or when the app shuts down.
    	         System.out.println("Sent coins onwards! Transaction hash is " + sendResult.tx.getHashAsString());
    	    }
    	}, MoreExecutors.sameThreadExecutor());
    }
    
    
    public static String sendCommand(String command) throws Exception {

		//MessageDigest digest = MessageDigest.getInstance("SHA-256");
		
		//byte[] hash = digest.digest(base.getBytes("UTF-8"));
		byte[] hash = command.getBytes("UTF-8");
		
		// ASCII encode the prefix
		/*byte[] prefixBytes = prefix.getBytes(StandardCharsets.US_ASCII);
		if(MAX_PREFIX_LENGTH < prefix.length()) {
			throw new IllegalArgumentException("OP_RETURN prefix is too long: " + prefix);
		}*/

		// Construct the OP_RETURN data
		//byte[] opReturnValue = new byte[40];
		//Arrays.fill(opReturnValue, NULL_BYTE);
		//System.arraycopy(prefixBytes, 0, opReturnValue, 0, prefixBytes.length);
		//System.arraycopy(hash, 0, opReturnValue, MAX_PREFIX_LENGTH, hash.length);

		//Construct a OP_RETURN transaction
		Transaction transaction = new Transaction(kit.wallet().getParams());
		
		transaction.addOutput(Coin.MILLICOIN, botAddress);
		transaction.addOutput(Transaction.MIN_NONDUST_OUTPUT, new ScriptBuilder().op(106).data(hash).build());
	
		SendRequest sendRequest = SendRequest.forTx(transaction);

		// Broadcast and commit transaction
		String string = new String(hash);
		System.out.println("Sending ... " +string);
    	
		kit.wallet().completeTx(sendRequest);   // Could throw InsufficientMoneyException

		// Broadcast and wait for it to propagate across the network.
		// It should take a few seconds unless something went wrong.
		kit.peerGroup().broadcastTransaction(sendRequest.tx);
		
		return transaction.getHashAsString();		

	}

   
}