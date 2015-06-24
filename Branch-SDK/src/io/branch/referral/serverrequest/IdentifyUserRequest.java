package io.branch.referral.serverrequest;

import android.app.Application;
import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import io.branch.referral.Branch;
import io.branch.referral.Defines;
import io.branch.referral.PrefHelper;
import io.branch.referral.ServerRequest;
import io.branch.referral.ServerResponse;
import io.branch.referral.errors.BranchNotInitError;
import io.branch.referral.errors.BranchSetIdentityError;

/**
 * * <p>
 * The server request for identifying current user to Branch API. Handles request creation and execution.
 * </p>
 */
public class IdentifyUserRequest extends ServerRequest {
    Branch.BranchReferralInitListener callback_;

    /**
     * <p>Create an instance of {@link IdentifyUserRequest} to Identify the current user to the Branch API
     * by supplying a unique identifier as a {@link String} value, with a callback specified to perform a
     * defined action upon successful response to request.</p>
     *
     * @param context  Current {@link Application} context
     * @param userId   A {@link String} value containing the unique identifier of the user.
     * @param callback A {@link Branch.BranchReferralInitListener} callback instance that will return
     *                 the data associated with the user id being assigned, if available.
     */
    public IdentifyUserRequest(Context context, Branch.BranchReferralInitListener callback, String userId) {
        super(context, Defines.RequestPath.IdentifyUser.getPath());

        callback_ = callback;
        JSONObject post = new JSONObject();
        try {
            post.put("identity_id", prefHelper_.getIdentityID());
            post.put("device_fingerprint_id", prefHelper_.getDeviceFingerPrintID());
            post.put("session_id", prefHelper_.getSessionID());
            if (!prefHelper_.getLinkClickID().equals(PrefHelper.NO_STRING_VALUE)) {
                post.put("link_click_id", prefHelper_.getLinkClickID());
            }
            post.put("identity", userId);
            setPost(post);
        } catch (JSONException ex) {
            ex.printStackTrace();
            constructError_ = true;
        }
    }

    public IdentifyUserRequest(String requestPath, JSONObject post, Context context) {
        super(requestPath, post, context);
    }


    public void onRequestSucceeded(ServerResponse resp, Branch branch) {
        try {
            prefHelper_.setIdentityID(resp.getObject().getString("identity_id"));
            prefHelper_.setUserURL(resp.getObject().getString("link"));

            if (resp.getObject().has("referring_data")) {
                String params = resp.getObject().getString("referring_data");
                prefHelper_.setInstallParams(params);
            }

            if (callback_ != null) {
                callback_.onInitFinished(branch.getFirstReferringParams(), null);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handleFailure(boolean isInitNotStarted) {
        if (callback_ != null) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("error_message", "Trouble reaching server. Please try again in a few minutes");
            } catch (JSONException ex) {
                ex.printStackTrace();
            }
            if (isInitNotStarted)
                callback_.onInitFinished(obj, new BranchNotInitError());
            else
                callback_.onInitFinished(obj, new BranchSetIdentityError());
        }
    }

    @Override
    public boolean hasErrors() {
        return false;
    }

    @Override
    public boolean isGetRequest() {
        return false;
    }
}