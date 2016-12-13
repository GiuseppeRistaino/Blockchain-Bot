import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionConfidence;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.utils.BriefLogFormatter;
import org.bitcoinj.wallet.SendRequest;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.listeners.WalletCoinsReceivedEventListener;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

public class Bot {

	private static Address masterAddress;
    private static WalletAppKit kit;
	
    public static void main(String[] args) throws Exception {
        // This line makes the log output more compact and easily read, especially when using the JDK log adapter.
        BriefLogFormatter.init();
        

        // Figure out which network we should connect to. Each one gets its own set of files.
        NetworkParameters params;
        String filePrefix;
        params = TestNet3Params.get();
        filePrefix = "forwarding-service-testnet";
        

        // Start up a basic app using a class that automates some boilerplate.
        kit = new WalletAppKit(params, new File("Bot"), filePrefix);
        
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
                System.out.println("Transaction will be forwarded after it confirms.");
                // Wait until it's made it into the block chain (may run immediately if it's already there).
                //
                // For this dummy app of course, we could just forward the unconfirmed transaction. If it were
                // to be double spent, no harm done. Wallet.allowSpendingUnconfirmedTransactions() would have to
                // be called in onSetupCompleted() above. But we don't do that here to demonstrate the more common
                // case of waiting for a block.
                
                System.out.println("Il Bot ha: " +w.getBalance().toFriendlyString());
                try {
					readOpReturn(tx);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                
                
                
                Futures.addCallback(tx.getConfidence().getDepthFuture(1), new FutureCallback<TransactionConfidence>() {
                    @Override
                    public void onSuccess(TransactionConfidence result) {
                        System.out.println("Transazione ricevuta con successo!!!!!!");
                        /*
                         * Inoltrare la transazione ricevuta nuovamente al BotMaster
                         */
                        forwardCommand("ATTIVO", tx);
                       
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        // This kind of future can't fail, just rethrow in case something weird happens.
                        throw new RuntimeException(t);
                    }
                });
            }
        });

        List<Address> list = kit.wallet().getWatchedAddresses();
        if (list.size() < 2) {
            kit.wallet().addWatchedAddress(kit.wallet().freshReceiveAddress());
            System.out.println("New address created");
        }

        System.out.println("You have " + list.size() + " addresses!");
        for (Address a: list) {
            System.out.println("Send coins to: " +a.toString());
        }

        String balance = kit.wallet().getBalance().toFriendlyString();
        System.out.println(balance);
        
        Address sendToAddress = kit.wallet().currentReceiveKey().toAddress(params);
        System.out.println("Send coins to: " + sendToAddress);
        System.out.println("Waiting for coins to arrive. Press Ctrl-C to quit.");

        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException ignored) {}
    }
    
    public static void readOpReturn(Transaction tx) throws Exception {
		List<TransactionOutput> ti = tx.getOutputs();
		
		for (TransactionOutput t : ti) {
			Script script = t.getScriptPubKey();
			
			if (script.isOpReturn()) {
				byte[] message = new byte[script.getProgram().length-2];
				
				System.arraycopy(script.getProgram(), 2, message, 0, script.getProgram().length-2);
					
				String string = new String(message);
				System.out.println(string);
				
			}
			
		}
		
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
    
    
    public static void forwardCommand(String response, Transaction tx) {
    	List<TransactionOutput> out = tx.getOutputs();
        //List<TransactionInput> in = tx.getInputs();
        
        Address a = null;
        for (TransactionOutput o : out) {
        	o.getAddressFromP2PKHScript(kit.wallet().getParams());
        	if (o.getAddressFromP2PKHScript(kit.wallet().getParams()) != null) {
        		a = o.getAddressFromP2PKHScript(kit.wallet().getParams());
        		break;
        	}
        }
        System.out.println("Hai ricevuto i Coins da: " + a.toString());
        
        try {
			sendCommand(response, a);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    
    
}
