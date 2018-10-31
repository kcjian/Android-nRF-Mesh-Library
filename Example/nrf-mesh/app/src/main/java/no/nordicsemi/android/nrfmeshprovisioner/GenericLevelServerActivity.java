package no.nordicsemi.android.nrfmeshprovisioner;

import android.arch.lifecycle.ViewModelProvider;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import javax.inject.Inject;

import no.nordicsemi.android.meshprovisioner.transport.GenericLevelGet;
import no.nordicsemi.android.meshprovisioner.transport.GenericLevelSet;
import no.nordicsemi.android.meshprovisioner.transport.GenericLevelStatus;
import no.nordicsemi.android.meshprovisioner.transport.MeshMessage;
import no.nordicsemi.android.meshprovisioner.transport.MeshModel;
import no.nordicsemi.android.meshprovisioner.transport.ProvisionedMeshNode;
import no.nordicsemi.android.meshprovisioner.models.GenericLevelServerModel;
import no.nordicsemi.android.meshprovisioner.utils.CompositionDataParser;
import no.nordicsemi.android.meshprovisioner.utils.Element;
import no.nordicsemi.android.meshprovisioner.utils.MeshParserUtils;

public class GenericLevelServerActivity extends BaseModelConfigurationActivity {

    private static final String TAG = GenericOnOffServerActivity.class.getSimpleName();

    @Inject
    ViewModelProvider.Factory mViewModelFactory;

    private TextView level;
    private TextView time;
    private TextView remainingTime;
    private SeekBar mTransitionTimeSeekBar;
    private SeekBar mDelaySeekBar;
    private SeekBar mLevelSeekBar;

    private int mTransitionStepResolution;
    private int mTransitionSteps;

    @Override
    protected final void addControlsUi(final MeshModel model) {
        if (model instanceof GenericLevelServerModel) {
            final CardView cardView = findViewById(R.id.node_controls_card);
            final View nodeControlsContainer = LayoutInflater.from(this).inflate(R.layout.layout_generic_level, cardView);
            time = nodeControlsContainer.findViewById(R.id.transition_time);
            remainingTime = nodeControlsContainer.findViewById(R.id.transition_state);
            mTransitionTimeSeekBar = nodeControlsContainer.findViewById(R.id.transition_seekbar);
            mTransitionTimeSeekBar.setProgress(0);
            mTransitionTimeSeekBar.incrementProgressBy(1);
            mTransitionTimeSeekBar.setMax(230);

            mDelaySeekBar = nodeControlsContainer.findViewById(R.id.delay_seekbar);
            mDelaySeekBar.setProgress(0);
            mDelaySeekBar.setMax(255);
            final TextView delayTime = nodeControlsContainer.findViewById(R.id.delay_time);

            level = nodeControlsContainer.findViewById(R.id.level);
            mLevelSeekBar = nodeControlsContainer.findViewById(R.id.level_seekbar);
            mLevelSeekBar.setProgress(0);
            mLevelSeekBar.setMax(100);

            mActionRead = nodeControlsContainer.findViewById(R.id.action_read);
            mActionRead.setOnClickListener(v -> {
                sendGenericLevelGet();
                showProgressbar();
            });

            mTransitionTimeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                int lastValue = 0;
                double res = 0.0;

                @Override
                public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {

                    if (progress >= 0 && progress <= 62) {
                        lastValue = progress;
                        mTransitionStepResolution = 0;
                        mTransitionSteps = progress;
                        res = progress / 10.0;
                        time.setText(getString(R.string.transition_time_interval, String.valueOf(res), "s"));
                    } else if (progress >= 63 && progress <= 118) {
                        if (progress > lastValue) {
                            mTransitionSteps = progress - 56;
                            lastValue = progress;
                        } else if (progress < lastValue) {
                            mTransitionSteps = -(56 - progress);
                        }
                        mTransitionStepResolution = 1;
                        time.setText(getString(R.string.transition_time_interval, String.valueOf(mTransitionSteps), "s"));

                    } else if (progress >= 119 && progress <= 174) {
                        if (progress > lastValue) {
                            mTransitionSteps = progress - 112;
                            lastValue = progress;
                        } else if (progress < lastValue) {
                            mTransitionSteps = -(112 - progress);
                        }
                        mTransitionStepResolution = 2;
                        time.setText(getString(R.string.transition_time_interval, String.valueOf(mTransitionSteps * 10), "s"));
                    } else if (progress >= 175 && progress <= 230) {
                        if (progress >= lastValue) {
                            mTransitionSteps = progress - 168;
                            lastValue = progress;
                        } else if (progress < lastValue) {
                            mTransitionSteps = -(168 - progress);
                        }
                        mTransitionStepResolution = 3;
                        time.setText(getString(R.string.transition_time_interval, String.valueOf(mTransitionSteps * 10), "min"));
                    }
                }

                @Override
                public void onStartTrackingTouch(final SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(final SeekBar seekBar) {

                }
            });

            mDelaySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {
                    delayTime.setText(getString(R.string.transition_time_interval, String.valueOf(progress * MeshParserUtils.GENERIC_ON_OFF_5_MS), "ms"));
                }

                @Override
                public void onStartTrackingTouch(final SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(final SeekBar seekBar) {

                }
            });

            mLevelSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {
                    level.setText(getString(R.string.generic_level_percent, progress));
                }

                @Override
                public void onStartTrackingTouch(final SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(final SeekBar seekBar) {
                    showProgressbar();
                    final int level = seekBar.getProgress();
                    final int delay = mDelaySeekBar.getProgress();
                    final int genericLevel = ((level * 65535) / 100) - 32768;
                    sendGenericLevel(genericLevel, delay);
                }
            });
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void enableClickableViews() {
        super.enableClickableViews();
        mTransitionTimeSeekBar.setEnabled(true);
        mDelaySeekBar.setEnabled(true);
        mLevelSeekBar.setEnabled(true);
    }

    @Override
    protected void disableClickableViews() {
        super.disableClickableViews();
        mTransitionTimeSeekBar.setEnabled(false);
        mDelaySeekBar.setEnabled(false);
        mLevelSeekBar.setEnabled(false);
    }

    @Override
    protected void updateMeshMessage(final MeshMessage meshMessage) {
        super.updateMeshMessage(meshMessage);
        if(meshMessage instanceof GenericLevelStatus){
            final GenericLevelStatus status = (GenericLevelStatus) meshMessage;
            hideProgressBar();
            final int presentLevel = status.getPresentLevel();
            final Integer targetLevel = status.getTargetLevel();
            final int steps = status.getTransitionSteps();
            final int resolution = status.getTransitionResolution();
            final int levelPercent;
            if (targetLevel == null) {
                levelPercent = ((presentLevel + 32768) * 100) / 65535;
                level.setText(getString(R.string.generic_level_percent, levelPercent));
                remainingTime.setVisibility(View.GONE);
            } else {
                levelPercent = ((targetLevel + 32768) * 100) / 65535;
                level.setText(getString(R.string.generic_level_percent, levelPercent));
                remainingTime.setText(getString(R.string.remaining_time, MeshParserUtils.getRemainingTransitionTime(resolution, steps)));
                remainingTime.setVisibility(View.VISIBLE);
            }
            mLevelSeekBar.setProgress(levelPercent);
        }
    }


    /**
     * Send generic on off get to mesh node
     */
    public void sendGenericLevelGet() {
        final ProvisionedMeshNode node = mViewModel.getSelectedMeshNode().getMeshNode();
        final Element element = mViewModel.getSelectedElement().getElement();
        final MeshModel model = mViewModel.getSelectedModel().getMeshModel();

        if (!model.getBoundAppKeyIndexes().isEmpty()) {
            final int appKeyIndex = model.getBoundAppKeyIndexes().get(0);
            final byte[] appKey = MeshParserUtils.toByteArray(model.getBoundAppKey(appKeyIndex));

            final byte[] address = element.getElementAddress();
            Log.v(TAG, "Sending message to element's unicast address: " + MeshParserUtils.bytesToHex(address, true));

            final GenericLevelGet genericLevelGet = new GenericLevelGet(node, appKey, 0);
            mViewModel.getMeshManagerApi().sendMeshApplicationMessage(address, genericLevelGet);
        } else {
            Toast.makeText(this, R.string.error_no_app_keys_bound, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Send generic level set to mesh node
     *
     * @param level                level
     * @param delay                message execution delay in 5ms steps. After this delay milliseconds the model will execute the required behaviour.
     */
    public void sendGenericLevel(final int level, final Integer delay) {
        final ProvisionedMeshNode node = mViewModel.getSelectedMeshNode().getMeshNode();
        final Element element = mViewModel.getSelectedElement().getElement();
        final MeshModel model = mViewModel.getSelectedModel().getMeshModel();

        if (!model.getBoundAppKeyIndexes().isEmpty()) {
            final int appKeyIndex = model.getBoundAppKeyIndexes().get(0);
            final byte[] appKey = MeshParserUtils.toByteArray(model.getBoundAppKey(appKeyIndex));
            if (!model.getSubscriptionAddresses().isEmpty()) {
                for(byte[] address : model.getSubscriptionAddresses()) {
                    final MeshMessage message;
                    if(!MeshParserUtils.isValidGroupAddress(address)) {
                        Log.v(TAG, "Subscription addresses found for model: " + CompositionDataParser.formatModelIdentifier(model.getModelId(), true)
                                + ". Sending acknowledged message to subscription address: " + MeshParserUtils.bytesToHex(address, true));
                        message = new GenericLevelSet(node, appKey, mTransitionSteps, mTransitionStepResolution, delay, level, 0);
                        mViewModel.getMeshManagerApi().sendMeshApplicationMessage(address, message);
                        showProgressbar();
                    } else {
                        Log.v(TAG, "Group subscription address found for model: " + CompositionDataParser.formatModelIdentifier(model.getModelId(), true)
                                + ". Sending unacknowledged message to subscription address: " + MeshParserUtils.bytesToHex(address, true));
                        message = new GenericLevelSet(node, appKey, mTransitionSteps, mTransitionStepResolution, delay, level, 0);
                        mViewModel.getMeshManagerApi().sendMeshApplicationMessage(address, message);
                    }
                }
            } else {
                final byte[] address = element.getElementAddress();
                Log.v(TAG, "No subscription addresses found for model: " + CompositionDataParser.formatModelIdentifier(model.getModelId(), true)
                        + ". Sending message to element's unicast address: " + MeshParserUtils.bytesToHex(address, true));
                final GenericLevelSet genericLevelSet = new GenericLevelSet(node, appKey, mTransitionSteps, mTransitionStepResolution, delay, level, 0);
                mViewModel.getMeshManagerApi().sendMeshApplicationMessage(address, genericLevelSet);
            }
            showProgressbar();
        } else {
            Toast.makeText(this, R.string.error_no_app_keys_bound, Toast.LENGTH_SHORT).show();
        }
    }
}
