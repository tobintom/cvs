package com.cvshealth.cvs;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.cvshealth.cvs.model.Passkey;
import com.cvshealth.cvs.service.HyprFIDO;

import java.util.ArrayList;

public class PasskeyAdapter extends RecyclerView.Adapter<PasskeyAdapter.ViewHolder>{
        private final Context context;
        public static final String PREFERENCES = "CVSPref" ;
        public static final String user = "nameKey";
        public static final String pwe = "pwe";
        SharedPreferences sharedpreferences;
        private final ArrayList<Passkey> passkeyArrayList;
        private  String username;

        // Constructor
        public PasskeyAdapter(Context context, ArrayList<Passkey> passkeyArrayList) {
            this.context = context;
            this.passkeyArrayList = passkeyArrayList;

        }

        @NonNull
        @Override
        public PasskeyAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.passkeycard, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PasskeyAdapter.ViewHolder holder, int position) {
            // to set data to textview and imageview of each card layout
            this.sharedpreferences = this.context.getSharedPreferences(MainActivity.PREFERENCES, Context.MODE_PRIVATE);
            this.username = sharedpreferences.getString(MainActivity.user,null);
            Passkey model = passkeyArrayList.get(position);
            holder.commonName.setText(model.getCommonName());
            holder.created.setText(model.getCreated());
            holder.last.setText(model.getLast());
            holder.credid.setText(model.getCredid());
            holder.del.setOnClickListener((r)->{
                new AlertDialog.Builder(this.context)
                        .setTitle("Title")
                        .setMessage("Do you really want to delete this passkey?")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                Thread thread = new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            HyprFIDO.deleteUserPasskey(username,model.getCredid());
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                });
                                thread.start();
                                passkeyArrayList.remove(holder.getAdapterPosition());
                                notifyItemRemoved(holder.getAdapterPosition());
                                notifyDataSetChanged();
                                android.app.AlertDialog alertDialog = new android.app.AlertDialog.Builder(context).create();
                                alertDialog.setTitle("Success");
                                alertDialog.setIcon(R.drawable.baseline_delete_24);
                                alertDialog.setMessage("The passkey registration has been deleted. Please remove the passkey from your Google Password manager as well.");
                                alertDialog.setButton(android.app.AlertDialog.BUTTON_POSITIVE, "OK",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.dismiss();
                                            }
                                        });
                                alertDialog.show();

                            }})
                        .setNegativeButton(android.R.string.no, null).show();

            });
        }

        @Override
        public int getItemCount() {
            return passkeyArrayList.size();
        }

// View holder class for initializing of your views such as TextView and Imageview
public static class ViewHolder extends RecyclerView.ViewHolder {
    private final TextView commonName;
    private final TextView created;
    private final TextView last;
    private final TextView credid;
    private final ImageView del;

    public ViewHolder(@NonNull View itemView) {
        super(itemView);
        commonName = itemView.findViewById(R.id.commonname);
        created = itemView.findViewById(R.id.created);
        last = itemView.findViewById(R.id.last);
        credid = itemView.findViewById(R.id.credid);
        del = itemView.findViewById(R.id.del);
    }
}
}
