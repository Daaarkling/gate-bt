package cz.janvanura.gate_bt;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


public class ChangeKeyFragment extends DialogFragment {

    private final static String TAG = GattAttributes.NAME;

    private EditText mEditTextSecure;

    private NoticeDialogListener mListener;



    public interface NoticeDialogListener {
        public void onChangeKey(String key);
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            mListener = (NoticeDialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement NoticeDialogListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_change_key, container);
        EditText editText = (EditText)view.findViewById(R.id.input_secure_key);

        // show soft keyboard
        editText.requestFocus();
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        return view;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.fragment_change_key, null);
        mEditTextSecure = (EditText) view.findViewById(R.id.input_secure_key);

        builder.setView(view);
        builder.setMessage(R.string.change_key_dialog_title)
                .setPositiveButton(R.string.btn_settings_confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Do nothing here because we override this button later to change the close behaviour.
                        // However, we still need this because on older versions of Android unless we
                        // pass a handler the button doesn't get instantiated
                    }
                })
                .setNegativeButton(R.string.btn_settings_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Log.i(TAG, "Change android secure key canceled");
                    }
                });
        return builder.create();
    }

    //onStart() is where dialog.show() is actually called on
    //the underlying dialog, so we have to do it there or
    //later in the lifecycle.
    //Doing it in onResume() makes sure that even if there is a config change
    //environment that skips onStart then the dialog will still be functioning
    //properly after a rotation.
    @Override
    public void onResume()
    {
        super.onResume();
        final AlertDialog dialog = (AlertDialog) getDialog();
        if(dialog != null) {
            Button positiveButton = dialog.getButton(Dialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    String secure = mEditTextSecure.getText().toString();
                    boolean error = false;

                    if (secure.contains(":")) {
                        error = true;
                        Toast.makeText(getActivity(), R.string.flesh_err_invalid_char, Toast.LENGTH_LONG).show();
                    }

                    if (secure.length() <= 0 || secure.length() >= 10) {
                        error = true;
                        Toast.makeText(getActivity(), R.string.flesh_err_input_secure, Toast.LENGTH_LONG).show();
                    }

                    if(!error) {
                        mListener.onChangeKey(secure);
                        dialog.dismiss();
                        Log.d(TAG, "Change secure key confirmed.");                    }
                }
            });
        }
    }
}
