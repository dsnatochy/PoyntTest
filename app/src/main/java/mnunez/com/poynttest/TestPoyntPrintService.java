package mnunez.com.poynttest;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;

import java.util.List;

import co.poynt.api.model.Transaction;
import co.poynt.os.model.Intents;
import co.poynt.os.model.PrintedReceipt;
import co.poynt.os.model.PrintedReceiptLine;
import co.poynt.os.services.v1.IPoyntReceiptPrintingService;
import co.poynt.os.services.v1.IPoyntReceiptPrintingServiceListener;

/**
 * Created by mnunez on 5/15/17.
 */

public class TestPoyntPrintService extends Service {

    private IPoyntReceiptPrintingService poyntReceiptPrintingService;

    private ServiceConnection receiptPrintingConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            poyntReceiptPrintingService = IPoyntReceiptPrintingService.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            poyntReceiptPrintingService = null;
            bindPoyntReceiptPrintingService();
        }
    };


    private final IPoyntReceiptPrintingService.Stub mBinder = new IPoyntReceiptPrintingService.Stub() {
        public void printTransaction(String jobId, Transaction transaction, long
                tipAmount, boolean signatureCollected, IPoyntReceiptPrintingServiceListener callback)
                throws RemoteException {
            // call default
            if (poyntReceiptPrintingService != null) {
                poyntReceiptPrintingService.printTransaction(jobId, transaction, tipAmount,
                        signatureCollected, callback);
            }
        }

        public void printTransactionReceipt(String jobId, String transactionId, long
                tipAmount, IPoyntReceiptPrintingServiceListener callback) throws RemoteException {
            // call default
            if (poyntReceiptPrintingService != null) {
                poyntReceiptPrintingService.printTransactionReceipt(jobId, transactionId, tipAmount,
                        callback);
            }
        }

        public void printOrderReceipt(String jobId, String orderId,
                                      IPoyntReceiptPrintingServiceListener callback) throws RemoteException {
            // call default
            if (poyntReceiptPrintingService != null) {
                poyntReceiptPrintingService.printOrderReceipt(jobId, orderId, callback);
            }
        }

        // This method will be called from the Payment Fragment
        public void printReceipt(final String jobId, final PrintedReceipt receipt,
                                 final IPoyntReceiptPrintingServiceListener callback) throws RemoteException {
            //Adding custom header for testing purposes
            List<PrintedReceiptLine> headerLines = receipt.getHeader();
            PrintedReceiptLine line = new PrintedReceiptLine();
            line.setText("This is an Accepta Test");
            headerLines.add(0, line);
            Handler h = new Handler(Looper.getMainLooper());
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    try {
                        poyntReceiptPrintingService.printReceipt(jobId, receipt, callback);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            };
            if (poyntReceiptPrintingService != null) {
                h.post(r);
            } else {
                h.postDelayed(r, 2000);
            }
        }

        public void printBitmap(String jobId, Bitmap bitmap, IPoyntReceiptPrintingServiceListener callback) throws RemoteException {
            if (poyntReceiptPrintingService != null) {
                poyntReceiptPrintingService.printBitmap(jobId, bitmap, callback);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        bindPoyntReceiptPrintingService();
    }

    private void bindPoyntReceiptPrintingService() {
        if (poyntReceiptPrintingService == null) {
            bindService(Intents.getComponentIntent(Intents.COMPONENT_POYNT_RECEIPT_PRINTING_SERVICE),
                    receiptPrintingConnection, BIND_AUTO_CREATE);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbindService(receiptPrintingConnection);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}
