package com.payu.sdk.fragments;


import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.InputFilter;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.payu.sdk.Constants;
import com.payu.sdk.Params;
import com.payu.sdk.PayU;
import com.payu.sdk.Payment;
import com.payu.sdk.PaymentListener;
import com.payu.sdk.R;
import com.payu.sdk.StoreCardTask;
import com.payu.sdk.adapters.StoredCardAdapter;

import org.apache.http.NameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 */

public class StoredCardFragment extends ProcessPaymentFragment implements PaymentListener {


    ProgressDialog mProgressDialog;

    String selectedItem = "Credit card";

    public StoredCardFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View storedCardFragment = inflater.inflate(R.layout.fragment_stored_card, container, false);

        mProgressDialog = new ProgressDialog(getActivity());

        return storedCardFragment;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mProgressDialog.setMessage(getString(R.string.please_wait));
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();

        getActivity().findViewById(R.id.useNewCardButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                final CharSequence[] cardTypes = {"Credit card", "Debit card"};

                new AlertDialog.Builder(getActivity())
                        .setTitle("Select card type")
                        .setSingleChoiceItems(cardTypes, 0, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if ("Credit card".equals(cardTypes[which])) {
                                    // replace credit card fragment
                                    selectedItem = "Credit card";
                                } else if ("Debit card".equals(cardTypes[which])) {
                                    // replace debit card fragment
                                    selectedItem = "Debit card";
                                }
                            }
                        }).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                        Bundle bundle = new Bundle();
                        bundle.putString(PayU.STORE_CARD, "store_card");
                        if (selectedItem.contentEquals("Credit card")) {
                            CreditCardDetailsFragment creditCardFragment = new CreditCardDetailsFragment();
                            creditCardFragment.setArguments(bundle);
                            transaction.replace(R.id.fragmentContainer, creditCardFragment);
                            transaction.addToBackStack(null);
                            transaction.commit();
                        } else if (selectedItem.contentEquals("Debit card")) {
                            DebitCardDetailsFragment debitCardDetailsFragment = new DebitCardDetailsFragment();
                            debitCardDetailsFragment.setArguments(bundle);
                            transaction.replace(R.id.fragmentContainer, debitCardDetailsFragment);
                            transaction.addToBackStack(null);
                            transaction.commit();
                        }
                    }
                }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Do nothing.
                    }

                }).show();

            }
        });

        List<NameValuePair> postParams = null;

        HashMap<String, String> varList = new HashMap<String, String>();
        varList.put(Constants.VAR1, getActivity().getIntent().getExtras().getString(PayU.USER_CREDENTIALS));

        try {
            postParams = PayU.getInstance(getActivity()).getParams(Constants.GET_USER_CARDS, varList);
            StoreCardTask getStoredCards = new StoreCardTask(StoredCardFragment.this);
            getStoredCards.execute(postParams);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPaymentOptionSelected(PayU.PaymentMode paymentMode) {

    }

    @Override
    public void onGetAvailableBanks(JSONArray response) {

    }

    @Override
    public void onGetStoreCardDetails(JSONArray storedCards) {

        StoredCardAdapter adapter = new StoredCardAdapter(getActivity(), storedCards);

        if (storedCards.length() < 1) {
            getActivity().findViewById(R.id.noCardFoundTextView).setVisibility(View.VISIBLE);
            getActivity().findViewById(R.id.savedCardTextView).setVisibility(View.GONE);
        }
        ListView listView = (ListView) getActivity().findViewById(R.id.storedCardListView);
        listView.setAdapter(adapter);

        mProgressDialog.dismiss();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                final JSONObject selectedCard = (JSONObject) adapterView.getAdapter().getItem(i);
                final EditText input = new EditText(getActivity());
                LinearLayout linearLayout = new LinearLayout(getActivity());
                linearLayout.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                layoutParams.setMargins(50, 0, 50, 0);
                input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                input.setTransformationMethod(PasswordTransformationMethod.getInstance());
                try {
                    input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(selectedCard.getString("card_no").startsWith("3") ? 4 : 3)});
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                input.setBackgroundResource(R.drawable.rectangle_box);
                input.setLines(1);
                input.setCompoundDrawablesWithIntrinsicBounds(null, null, getActivity().getResources().getDrawable(R.drawable.lock), null);
                linearLayout.addView(input, layoutParams);

                new AlertDialog.Builder(getActivity())
                        .setTitle(Constants.CVV_TITLE)
                        .setMessage(Constants.CVV_MESSAGE)
                        .setView(linearLayout)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
//                                    String cvv, String cardNumber, String nameOnCard, int expiryMonth, int expiryYear
                                //                                    makePayment(input.getText().toString(), selectedCard.getString("card_no"), selectedCard.getString("name_on_card"), selectedCard.getString("card_token"), Integer.parseInt(selectedCard.getString("expiry_month")), Integer.parseInt(selectedCard.getString("expiry_year")));
                                if (input.getText().length() > 2)
                                    makePayment(selectedCard, input.getText().toString());
                                else
                                    Toast.makeText(getActivity(), Constants.CVV_ERROR_MESSAGE, Toast.LENGTH_LONG).show();
                            }
                        }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Do nothing.
                    }
                }).show();
            }
        });
    }

    private void makePayment(JSONObject selectedCard, String cvv) {
        Params requiredParams = new Params();
        try {

            requiredParams.put(PayU.CVV, cvv);

            requiredParams.put("store_card_token", selectedCard.getString("card_token"));

            requiredParams.put(PayU.FIRSTNAME, selectedCard.getString("name_on_card"));

            startPaymentProcessActivity(PayU.PaymentMode.valueOf(selectedCard.getString("card_mode")), requiredParams);


        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
