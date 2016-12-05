package com.vrexas.bitcointest2;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionConfidence;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.listeners.WalletCoinsReceivedEventListener;

import java.io.File;

public class MainActivity extends AppCompatActivity {


    NetworkParameters params;
    String filePrefix;
    private static Address botAddress1, botAddress2;
    private static WalletAppKit kit;

    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = (TextView) this.findViewById(R.id.text);

        init();

    }


    private void init() {

        params = TestNet3Params.get();
        filePrefix = "forwarding-service-testnet";

        //botAddress = Address.fromBase58(params, "mkXJLEzLrqkEHAU6XjnxcpBdAsvhfAj5ow");
        botAddress1 = new Address(params, "mgXaam8xQx1HiQnpKW5ana5jnsPEzc4uQZ");
        botAddress2 = new Address(params, "muMWvMjKBcbSorRNaMeQsWhg1oQ9S44LMz");

        File file = new File(Environment.getExternalStorageDirectory(), "/Master/");
        if (!file.exists()) {
            if (!file.mkdirs()) {
                Toast.makeText(MainActivity.this, "Errore nella creazione della directory", Toast.LENGTH_SHORT).show();
            }
        }

        Toast.makeText(MainActivity.this, "Directory creata correttamente", Toast.LENGTH_SHORT).show();


        kit = new WalletAppKit(params, file, filePrefix);



        //Log.d("Debug", "Sto scaricando la blockchain");


        //IDEALE ----> Almeno all'inizio utilizzare il task per scaricare la blockchain
        //new HttpAsyncTask().execute();

        //Meglio non metterli qua....
        kit.startAsync();
        kit.awaitRunning();

        //Log.d("Debug", "Ho scaricato la blockchain");
        Toast.makeText(MainActivity.this, "Ho scaricato la blockchain", Toast.LENGTH_SHORT).show();


        kit.wallet().addCoinsReceivedEventListener(new WalletCoinsReceivedEventListener() {
            @Override
            public void onCoinsReceived(Wallet w, Transaction tx, Coin prevBalance, Coin newBalance) {
                Coin value = tx.getValueSentToMe(w);
                Toast.makeText(MainActivity.this, "Received tx for " + value.toFriendlyString() + ": " + tx, Toast.LENGTH_SHORT).show();

                Futures.addCallback(tx.getConfidence().getDepthFuture(1), new FutureCallback<TransactionConfidence>() {
                    @Override
                    public void onSuccess(TransactionConfidence result) {
                        Toast.makeText(MainActivity.this, "Transazione ricevuta con successo", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        // This kind of future can't fail, just rethrow in case something weird happens.
                        Toast.makeText(MainActivity.this, "Transazione non ricevuta", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });


        Address sendToAddress = kit.wallet().currentReceiveKey().toAddress(params);
        String string = "Indirizzo del Master: " + sendToAddress;
        String s2 = "Il Master ha: " +kit.wallet().getBalance().toFriendlyString();
        textView.setText(string);
        textView.append("\n" +s2);



    }

    /*
        L'ideale sarebbe usare questo Task ma occorre passare l'oggetto WalletAppKit alla prossima activity
     */
    private class HttpAsyncTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            //Toast.makeText(MainActivity.this, "Sto scaricando la blockchain", Toast.LENGTH_SHORT).show();
            kit.startAsync();
            kit.awaitRunning();
            return "Blockchain";
        }

        @Override
        protected void onPostExecute(String s) {
            Toast.makeText(MainActivity.this, "Ho scaricato la blockchain" +s, Toast.LENGTH_SHORT).show();

            /*
                   RICHIAMARE LA PROSSIMA ACTIVITY (LISTA DEI BOT) QUI E PASSARLE L'OGGETTO WalletAppKit
                   Ma non so come fare...forse Content Provider???
             */

            Address sendToAddress = kit.wallet().currentReceiveKey().toAddress(params);
            String string = "Indirizzo del Master: " + sendToAddress;
            textView.setText(string);

        }

    }

}
