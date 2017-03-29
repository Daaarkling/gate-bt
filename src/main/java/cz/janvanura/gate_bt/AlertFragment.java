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
import android.widget.EditText;


public class AlertFragment extends DialogFragment {

    private final static String TAG = GattAttributes.NAME;

    private NoticeDialogListener mListener;



    public interface NoticeDialogListener {
        public void onDialogPositiveClick(String key);
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

        View view = inflater.inflate(R.layout.fragment_alert, container);
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
        View view = inflater.inflate(R.layout.fragment_alert, null);
        final EditText editTextKey = (EditText) view.findViewById(R.id.input_secure_key);

        builder.setView(view);
        builder.setMessage(R.string.settings_dialog_title)
                .setPositiveButton(R.string.btn_settings_confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mListener.onDialogPositiveClick(editTextKey.getText().toString());
                        Log.d(TAG, editTextKey.getText().toString());
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
}
