package io.branch.referral.serverrequest;

import android.app.Application;
import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;

import io.branch.referral.Branch;
import io.branch.referral.BranchError;
import io.branch.referral.BranchLinkData;
import io.branch.referral.Defines;
import io.branch.referral.PrefHelper;
import io.branch.referral.ServerRequest;
import io.branch.referral.ServerResponse;

/**
 * * <p>
 * The server request for creating a  synchronous or asynchronous short url. Handles request creation and execution.
 * </p>
 */
public class CreateUrlRequest extends ServerRequest {

    BranchLinkData linkPost_;
    boolean isAsync_ = true;
    Branch.BranchLinkCreateListener callback_;

    /**
     * <p>Configures and requests a short URL to be generated by the Branch servers, via a synchronous
     * call; with a duration specified within which an app session should be matched to the link.</p>
     *
     * @param context  Current {@link Application} context
     * @param alias    Link 'alias' can be used to label the endpoint on the link.
     *                 <p/>
     *                 <p>
     *                 For example:
     *                 http://bnc.lt/AUSTIN28.
     *                 Should not exceed 128 characters
     *                 </p>
     * @param type     An {@link int} that can be used for scenarios where you want the link to
     *                 only deep link the first time.
     * @param duration A {@link Integer} value specifying the time that Branch allows a click to
     *                 remain outstanding and be eligible to be matched with a new app session
     * @param tags     An iterable {@link Collection} of {@link String} tags associated with a deep
     *                 link.
     * @param channel  A {@link String} denoting the channel that the link belongs to. Should not
     *                 exceed 128 characters.
     * @param feature  A {@link String} value identifying the feature that the link makes use of.
     *                 Should not exceed 128 characters.
     * @param stage    A {@link String} value identifying the stage in an application or user flow
     *                 process. Should not exceed 128 characters.
     * @param params   A {@link JSONObject} value containing the deep linked params associated with
     *                 the link that will be passed into a new app session when clicked
     * @param callback A {@link Branch.BranchLinkCreateListener} callback instance that will trigger
     *                 actions defined therein upon receipt of a response to a create link request.
     * @param async    {@link Boolean} value specifying whether to get the url asynchronously or not.     *
     * @return A {@link String} containing the resulting short URL.
     */
    public CreateUrlRequest(Context context, final String alias, final int type, final int duration,
                            final Collection<String> tags, final String channel, final String feature,
                            final String stage, final String params,
                            Branch.BranchLinkCreateListener callback, boolean async) {

        super(context, Defines.RequestPath.GetURL.getPath());

        callback_ = callback;
        isAsync_ = async;

        linkPost_ = new BranchLinkData();
        try {
            linkPost_.put("identity_id", prefHelper_.getIdentityID());
            linkPost_.put("device_fingerprint_id", prefHelper_.getDeviceFingerPrintID());
            linkPost_.put("session_id", prefHelper_.getSessionID());
            if (!prefHelper_.getLinkClickID().equals(PrefHelper.NO_STRING_VALUE)) {
                linkPost_.put("link_click_id", prefHelper_.getLinkClickID());
            }

            linkPost_.putType(type);
            linkPost_.putDuration(duration);
            linkPost_.putTags(tags);
            linkPost_.putAlias(alias);
            linkPost_.putChannel(channel);
            linkPost_.putFeature(feature);
            linkPost_.putStage(stage);
            linkPost_.putParams(params);
            setPost(linkPost_);

        } catch (JSONException ex) {
            ex.printStackTrace();
            constructError_ = true;
        }

    }

    public CreateUrlRequest(String requestPath, JSONObject post, Context context) {
        super(requestPath, post, context);
    }

    public BranchLinkData getLinkPost() {
        return linkPost_;
    }


    @Override
    public boolean handleErrors(Context context) {
        if (!super.doesAppHasInternetPermission(context)) {
            callback_.onLinkCreate(null, new BranchError("Trouble creating a URL.", BranchError.ERR_NO_INTERNET_PERMISSION));
            return true;
        }
        if (!isAsync_ && !hasUser()) {
            return true;
        }
        return false;
    }

    @Override
    public void onRequestSucceeded(ServerResponse resp, Branch branch) {
        try {
            final String url = resp.getObject().getString("url");
            if (callback_ != null) {
                callback_.onLinkCreate(url, null);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void handleFailure(int statusCode) {
        if (callback_ != null) {
            String failedUrl = null;
            if (!prefHelper_.getUserURL().equals(PrefHelper.NO_STRING_VALUE)) {
                failedUrl = prefHelper_.getUserURL();
            }

            if (statusCode == BranchError.ERR_NO_SESSION)
                callback_.onLinkCreate(null, new BranchError("Trouble creating a URL.", statusCode));
            else
                callback_.onLinkCreate(failedUrl, new BranchError("Trouble creating a URL.", statusCode));
        }
    }

    public void handleDuplicateURLError() {
        if (callback_ != null) {
            callback_.onLinkCreate(null, new BranchError("Trouble creating a URL.", BranchError.ERR_BRANCH_DUPLICATE_URL));
        }
    }

    private boolean hasUser() {
        return !prefHelper_.getIdentityID().equals(PrefHelper.NO_STRING_VALUE);
    }

    @Override
    public boolean isGetRequest() {
        return false;
    }

    @Override
    public void clearCallbacks() {
        callback_ = null;
    }
}
