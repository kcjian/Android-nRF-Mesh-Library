/*
 * Copyright (c) 2018, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package no.nordicsemi.android.nrfmeshprovisioner.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.KeyListener;
import android.view.LayoutInflater;
import android.view.View;

import butterknife.BindView;
import butterknife.ButterKnife;
import no.nordicsemi.android.meshprovisioner.utils.MeshParserUtils;
import no.nordicsemi.android.meshprovisioner.utils.SecureUtils;
import no.nordicsemi.android.nrfmeshprovisioner.R;
import no.nordicsemi.android.nrfmeshprovisioner.utils.HexKeyListener;
import no.nordicsemi.android.nrfmeshprovisioner.utils.Utils;

public class DialogFragmentNetworkKey extends DialogFragment {

    private static final String PATTERN_NETWORK_KEY = "[0-9a-fA-F]{32}";
    private static final String NETWORK_KEY = "NETWORK_KEY";

    //UI Bindings
    @BindView(R.id.text_input_layout)
    TextInputLayout networkKeyInputLayout;
    @BindView(R.id.text_input)
    TextInputEditText networkKeyInput;

    private String mNetworkKey;

    public static DialogFragmentNetworkKey newInstance(final String networkKey) {
        DialogFragmentNetworkKey fragmentNetworkKey = new DialogFragmentNetworkKey();
        final Bundle args = new Bundle();
        args.putString(NETWORK_KEY, networkKey);
        fragmentNetworkKey.setArguments(args);
        return fragmentNetworkKey;
    }

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mNetworkKey = getArguments().getString(NETWORK_KEY);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        final View rootView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_fragment_key_input, null);

        final KeyListener hexKeyListener = new HexKeyListener();
        //Bind ui
        ButterKnife.bind(this, rootView);
        networkKeyInputLayout.setHint(getString(R.string.hint_network_key));
        networkKeyInput.setKeyListener(hexKeyListener);
        networkKeyInput.setText(mNetworkKey);
        networkKeyInput.setSelection(mNetworkKey.length());
        networkKeyInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {

            }

            @Override
            public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
                if (TextUtils.isEmpty(s.toString())) {
                    networkKeyInputLayout.setError(getString(R.string.error_empty_network_key));
                } else {
                    networkKeyInputLayout.setError(null);
                }
            }

            @Override
            public void afterTextChanged(final Editable s) {

            }
        });

        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getContext()).setView(rootView)
                .setPositiveButton(R.string.ok, null).setNegativeButton(R.string.cancel, null)
                .setNeutralButton(R.string.generate_network_key, null);

        alertDialogBuilder.setIcon(R.drawable.ic_vpn_key_black_alpha_24dp);
        alertDialogBuilder.setTitle(R.string.title_generate_network_key);
        alertDialogBuilder.setMessage(R.string.summary_generate_network_key);

        final AlertDialog alertDialog = alertDialogBuilder.show();
        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            final String networkKey = networkKeyInput.getText().toString();
            if (validateInput(networkKey)) {
                if(getParentFragment() == null) {
                    ((DialogFragmentNetworkKeyListener) getActivity()).onNetworkKeyGenerated(networkKey);
                } else {
                    ((DialogFragmentNetworkKeyListener) getParentFragment()).onNetworkKeyGenerated(networkKey);
                }
                dismiss();
            }
        });
        alertDialog.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener(v -> networkKeyInput.setText(SecureUtils.generateRandomNetworkKey()));

        return alertDialog;
    }

    private boolean validateInput(final String input) {
        try {

            if(!input.matches(Utils.HEX_PATTERN)) {
                networkKeyInputLayout.setError(getString(R.string.invalid_hex_value));
                return false;
            }

            if(MeshParserUtils.validateNetworkKeyInput(getContext(), input)){
                return true;
            }
        } catch (IllegalArgumentException ex) {
            networkKeyInputLayout.setError(ex.getMessage());
        }
        return false;
    }

    public interface DialogFragmentNetworkKeyListener {

        void onNetworkKeyGenerated(final String networkKey);

    }
}
