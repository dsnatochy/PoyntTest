package mnunez.com.poynttest;

import android.app.AlertDialog;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;

import java.util.List;
import java.util.UUID;

import co.poynt.api.model.Transaction;
import co.poynt.os.model.AccessoryProvider;
import co.poynt.os.model.AccessoryProviderFilter;
import co.poynt.os.model.AccessoryType;
import co.poynt.os.model.Intents;
import co.poynt.os.model.PoyntError;
import co.poynt.os.model.PrintedReceipt;
import co.poynt.os.model.PrintedReceiptLine;
import co.poynt.os.model.PrinterStatus;
import co.poynt.os.services.v1.IPoyntAccessoryManager;
import co.poynt.os.services.v1.IPoyntAccessoryManagerListener;
import co.poynt.os.services.v1.IPoyntPrinterService;
import co.poynt.os.services.v1.IPoyntPrinterServiceListener;
import co.poynt.os.services.v1.IPoyntReceiptPrintingService;
import co.poynt.os.services.v1.IPoyntReceiptPrintingServiceListener;

/**
 * Created by mnunez on 5/15/17.
 */

public class TestPoyntPrintService extends Service {

    private static final String TAG = TestPoyntPrintService.class.getSimpleName();

    private IPoyntPrinterService poyntPrinterService;
    private ServiceConnection poyntPrinterConnection;
    private IPoyntAccessoryManager accessoryManagerService;

    private ServiceConnection accessoryManagerConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            accessoryManagerService = IPoyntAccessoryManager.Stub.asInterface(service);
            // if you want to use the built-in printer to print your custom receipt
            connectToPoyntPrinter();
            Log.d(TAG, "accessoryManagerConnection onServiceConnected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "accessoryManagerConnection onServiceDisconnected, reconnecting...");
            connectToPoyntPrinter();
        }
    };



    private final IPoyntReceiptPrintingService.Stub mBinder = new IPoyntReceiptPrintingService.Stub() {

        public void printTransaction(String jobId, Transaction transaction, long
                tipAmount, boolean signatureCollected, IPoyntReceiptPrintingServiceListener callback)
                throws RemoteException {
            Log.d(TAG, "printTransaction ");
            // your code
        }

        public void printTransactionReceipt(String jobId, String transactionId, long
                tipAmount, IPoyntReceiptPrintingServiceListener callback) throws RemoteException {
            Log.d(TAG, "printTransactionReceipt ");
            // your code
        }

        public void printOrderReceipt(String jobId, String orderId,
                                      IPoyntReceiptPrintingServiceListener callback) throws RemoteException {
            Log.d(TAG, "printOrderReceipt ");
            // your code
        }

        // This method will be called from the Payment Fragment
        public void printReceipt(String jobId, PrintedReceipt receipt,
                                 final IPoyntReceiptPrintingServiceListener callback) throws RemoteException {
            Log.d(TAG, "printReceipt " + accessoryManagerService);

            // Add a custom line at the header just for testing purpose
            List<PrintedReceiptLine> headerLines = receipt.getHeader();
            PrintedReceiptLine line = new PrintedReceiptLine();
            line.setText("This is an Accepta Test");
            headerLines.add(0, line);

            // If you want to print to the built-in printer you will need to construct
            // a bitmap of your custom receipt. In this case I am just printing a poynt logo
            try {
                poyntPrinterService.printJob(UUID.randomUUID().toString(),
                        BitmapFactory.decodeResource(getResources(), R.drawable.poynt_logo_300dpi_50x50),
                        new IPoyntPrinterServiceListener.Stub() {
                            @Override
                            public void onPrintResponse(PrinterStatus printerStatus, String s) throws RemoteException {
                                switch (printerStatus.getCode()){
                                    case PRINTER_CONNECTED:
                                        Log.d(TAG, "onPrintResponse: PRINTER_CONNECTED");
                                        break;
                                    case PRINTER_DISCONNECTED:
                                        Log.d(TAG, "onPrintResponse: PRINTER_DISCONNECTED");
                                        break;
                                    case PRINTER_UNAVAILABLE:
                                        Log.d(TAG, "onPrintResponse: PRINTER_UNAVAILABLE");
                                        // notify the callback function
                                        callback.printFailed();
                                        break;
                                    case PRINTER_JOB_PRINTED:
                                        Log.d(TAG, "onPrintResponse: PRINTER_JOB_PRINTED");
                                        break;
                                    case PRINTER_JOB_FAILED:
                                        Log.d(TAG, "onPrintResponse: PRINTER_JOB_FAILED");
                                        callback.printFailed();
                                        break;
                                    case PRINTER_JOB_QUEUED:
                                        Log.d(TAG, "onPrintResponse: PRINTER_JOB_QUEUED");
                                        callback.printQueued();
                                        break;
                                    case PRINTER_ERROR_OUT_OF_PAPER:
                                        Log.d(TAG, "onPrintResponse: PRINTER_ERROR_OUT_OF_PAPER");
                                        callback.printFailed();
                                        break;
                                    case PRINTER_ERROR_OTHER:
                                        Log.d(TAG, "onPrintResponse: PRINTER_ERROR_OTHER");
                                        callback.printFailed();
                                        break;
                                    default:
                                        Log.d(TAG, "onPrintResponse: This should not happen");
                                        callback.printFailed();
                                        break;
                                }

                            }
                        });
            } catch (RemoteException e) {
                e.printStackTrace();
                callback.printFailed();
            }
        }

        public void printBitmap(String jobId, Bitmap bitmap,
                                IPoyntReceiptPrintingServiceListener callback) throws RemoteException {
            Log.d(TAG, "printBitmap ");
            // your code
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        bindService(new Intent(IPoyntAccessoryManager.class.getName()), accessoryManagerConnection, BIND_AUTO_CREATE);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbindService(accessoryManagerConnection);
    }

    public TestPoyntPrintService() {
        Log.d(TAG, "constructor");
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind: called");
        return mBinder;
    }

    private void connectToPoyntPrinter(){
        AccessoryProviderFilter filter = new AccessoryProviderFilter(AccessoryType.PRINTER);
        try {
            accessoryManagerService.getAccessoryProviders(filter, new IPoyntAccessoryManagerListener.Stub() {
                @Override
                public void onError(PoyntError poyntError) throws RemoteException {
                    Log.d(TAG, poyntError.getReason());
                }

                @Override
                public void onSuccess(List<AccessoryProvider> printers) throws RemoteException {
                    Log.d(TAG, "onSuccess: getAccessoryProvider");
                    for (AccessoryProvider printer : printers) {
                        Log.d(TAG, "---------------------------------------");
                        Log.d(TAG, "printer name: " + printer.getProviderName());
                        Log.d(TAG, "printer id: " + printer.getId());
                        Log.d(TAG, "printer class name: " + printer.getClassName());
                        Log.d(TAG, "printer package name: " + printer.getPackageName());
                        Log.d(TAG, "printer is connected: " + printer.isConnected());


                        // select build-in Poynt printer and make sure it is not disabled
                        if (printer.isConnected() &&
                                "co.poynt.services.PoyntPrinterService".equals(printer.getClassName())) {
                            Intent intent = new Intent();
                            intent.setClassName(printer.getPackageName(), printer.getClassName());

                            poyntPrinterConnection = new ServiceConnection() {
                                @Override
                                public void onServiceConnected(ComponentName name, IBinder service) {
                                    poyntPrinterService = IPoyntPrinterService.Stub.asInterface(service);
                                }
                                @Override
                                public void onServiceDisconnected(ComponentName name) {
                                    Log.d(TAG, "onServiceDisconnected: poynt printer connection");
                                }
                            };

                            bindService(intent,
                                    poyntPrinterConnection, Context.BIND_AUTO_CREATE);

                            break;
                        }
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
