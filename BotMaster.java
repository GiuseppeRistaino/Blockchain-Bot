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
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.utils.BriefLogFormatter;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.listeners.WalletCoinsReceivedEventListener;
import org.bitcoinj.wallet.SendRequest;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;

/**
 * ForwardingService demonstrates basic usage of the library. It sits on the network and when it receives coins, simply
 * sends them onwards to an address given on the command line.
 */
public class BotMaster {
    private static Address botAddress1, botAddress2;
    private static WalletAppKit kit;
    private static File walletFile;
    

    public static void main(String[] args) throws Exception {
        init();
        
        
    }
    
    /*
     * Inizializza il portafoglio, la rete bitcoin, gli indirizzi dei bot e scarica la blockchain 
     */
    public static void init() {
    	
    	//Evitare in Android
    	BriefLogFormatter.init();

        NetworkParameters params;
        String filePrefix;
        params = TestNet3Params.get();
        filePrefix = "forwarding-service-testnet";
        
        //botAddress = Address.fromBase58(params, "mkXJLEzLrqkEHAU6XjnxcpBdAsvhfAj5ow");
        botAddress1 = new Address(params, "mgXaam8xQx1HiQnpKW5ana5jnsPEzc4uQZ");
        botAddress2 = new Address(params, "muMWvMjKBcbSorRNaMeQsWhg1oQ9S44LMz");

        walletFile = new File("Master.wallet");
        
        kit = new WalletAppKit(params, walletFile, filePrefix);

        // Download the block chain and wait until it's done.
        kit.startAsync();
        kit.awaitRunning();
        
        kit.wallet().addCoinsReceivedEventListener(new WalletCoinsReceivedEventListener() {
            @Override
            public void onCoinsReceived(Wallet w, Transaction tx, Coin prevBalance, Coin newBalance) {
                Coin value = tx.getValueSentToMe(w);
                System.out.println("Received tx for " + value.toFriendlyString() + ": " + tx);
                
                Futures.addCallback(tx.getConfidence().getDepthFuture(1), new FutureCallback<TransactionConfidence>() {
                    @Override
                    public void onSuccess(TransactionConfidence result) {
                        System.out.println("Transazione ricevuta con successo");
                        System.out.println("Il Master ha: " +w.getBalance().toFriendlyString());
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        // This kind of future can't fail, just rethrow in case something weird happens.
                        System.out.println("Transazione non ricevuta");
                    }
                });
            }
        });
        

        Address sendToAddress = kit.wallet().currentReceiveKey().toAddress(params);
        System.out.println("Indirizzo del Master: " + sendToAddress);
        System.out.println("Il Master ha: " +kit.wallet().getBalance().toFriendlyString());
        System.out.println("Waiting for coins to arrive. Press Ctrl-C to quit.");
        
        
        /*
         * Inserire al di fuori di questo metodo
         */
        
        try {
			sendCommand("Ciao", botAddress1);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
        
        /*
         * Inserire al di fuori di questo metodo
         */
        /*
        ArrayList<Address> list = new ArrayList<Address>();
        list.add(botAddress1);
        list.add(botAddress2);
        try {
			sendCommand("Broadcast", list);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        */
        
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException ignored) {}
    }
    
    
    /*
     * Invio di un comando a un bot
     */
    public static String sendCommand(String command, Address botAddress) throws Exception {

		byte[] hash = command.getBytes("UTF-8");
		
		Transaction transaction = new Transaction(kit.wallet().getParams());
		
		transaction.addOutput(Coin.MILLICOIN, botAddress);
		transaction.addOutput(Coin.ZERO, new ScriptBuilder().op(106).data(hash).build());
	
		SendRequest sendRequest = SendRequest.forTx(transaction);

		String string = new String(hash);
		System.out.println("Sending ... " +string);
    	
		kit.wallet().completeTx(sendRequest);   // Could throw InsufficientMoneyException

		kit.peerGroup().setMaxConnections(1);
		kit.peerGroup().broadcastTransaction(sendRequest.tx);
		
		return transaction.getHashAsString();		
	}

    
    /*
     * Invio di un comando a una lista di bot
     */
    public static String sendCommand(String command, ArrayList<Address> botAddressList) throws Exception {

		byte[] hash = command.getBytes("UTF-8");
		
		Transaction transaction = new Transaction(kit.wallet().getParams());
		
		for (Address address : botAddressList) {
			transaction.addOutput(Coin.MILLICOIN, address);
		}
		transaction.addOutput(Coin.ZERO, new ScriptBuilder().op(106).data(hash).build());
	
		SendRequest sendRequest = SendRequest.forTx(transaction);

		String string = new String(hash);
		System.out.println("Sending ... " +string);
    	
		kit.wallet().completeTx(sendRequest);   // Could throw InsufficientMoneyException

		kit.peerGroup().setMaxConnections(botAddressList.size());
		kit.peerGroup().broadcastTransaction(sendRequest.tx);
		
		return transaction.getHashAsString();		
	}
    
    
   
}