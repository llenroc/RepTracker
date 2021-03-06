package com.pioneerx1.reptracker.ui;


import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.pioneerx1.reptracker.Constants;
import com.pioneerx1.reptracker.R;
import com.pioneerx1.reptracker.adapters.VoteListAdapter;
import com.pioneerx1.reptracker.models.Rep;
import com.pioneerx1.reptracker.models.Vote;
import com.pioneerx1.reptracker.services.ProPublicaService;

import org.parceler.Parcels;

import java.io.IOException;
import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * A simple {@link Fragment} subclass.
 */
public class RepDetailFragment extends Fragment implements View.OnClickListener {

    //@Bind(R.id.repDetailNameTextView) TextView mRepNameTextView;
    //@Bind(R.id.repDetailMemberIdTextView) TextView mRepMemberIdTextView;
    @Bind(R.id.repDetailTitleTextView) TextView mRepTitleTextView;
    @Bind(R.id.repDetailPartyTextView) TextView mRepPartyTextView;
    @Bind(R.id.repDetailStateTextView) TextView mRepStateTextView;
    @Bind(R.id.repDetailPhoneTextView) TextView mRepPhoneTextView;
    @Bind(R.id.repDetailWebsiteTextView) TextView mRepWebsiteTextView;
    //@Bind(R.id.repDetailTwitterTextView) TextView mRepTwitterTextView;
    //@Bind(R.id.repDetailFacebookTextView) TextView mRepFacebookTextView;
    @Bind(R.id.repDetailMissedVotesTextView) TextView mRepMissedVotesTextView;
    @Bind(R.id.repDetailVotesWithPartyTextView) TextView mRepVotesWithPartyTextView;
    @Bind(R.id.repDetailNextElectionTextView) TextView mRepNextElectionTextView;

    @Bind(R.id.saveRepButton) Button mSaveRepButton;
    @Bind(R.id.votesRecyclerView) RecyclerView mVotesRecyclerView;
    //@Bind(R.id.detailScrollView) ScrollView mScrollView;

    private Rep mRep;
    private ArrayList<Vote> mVotes = new ArrayList<>();
    private VoteListAdapter mVoteAdapter;
    private String saveButtonOption = "";


    public static RepDetailFragment newInstance(Rep rep) {
        RepDetailFragment repDetailFragment = new RepDetailFragment();
        Bundle args = new Bundle();
        args.putParcelable("rep", Parcels.wrap(rep));
        repDetailFragment.setArguments(args);
        return repDetailFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRep = Parcels.unwrap(getArguments().getParcelable("rep"));
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_rep_detail, container, false);
        ButterKnife.bind(this, view);

        // top focus the ScrollView since size of Votes recycler view is large
        //mScrollView.fullScroll(ScrollView.FOCUS_UP);

        //mRepNameTextView.setText("Name: " + mRep.getName());
        //mRepMemberIdTextView.setText("Member ID: " + mRep.getMemberId());
        mRepTitleTextView.setText("Title: " + mRep.getTitle());
        mRepPartyTextView.setText("Party: " + mRep.getParty());
        mRepStateTextView.setText("State: " + mRep.getState());
        mRepPhoneTextView.setText("Phone: " + mRep.getPhone());
        mRepWebsiteTextView.setText("Website: " + mRep.getWebsite());
        //mRepFacebookTextView.setText("Facebook Account: " + mRep.getFacebookAccount());
        //mRepTwitterTextView.setText("Twitter Handle: " + mRep.getTwitterHandle());
        mRepMissedVotesTextView.setText("Percent of Missed Votes: " + mRep.getMissedVotes() + "%");
        checkMissedVotesColor();
        mRepVotesWithPartyTextView.setText("Percent of Time Votes with Party: " + mRep.getVotesWithParty() + "%");
        mRepNextElectionTextView.setText("Next Election Year: " + mRep.getNextElection());

        getVotes(mRep.getMemberId());

        Intent intent = getActivity().getIntent();
        saveButtonOption = intent.getStringExtra("activity");

        mRepPhoneTextView.setOnClickListener(this);
        mRepWebsiteTextView.setOnClickListener(this);

        if (saveButtonOption.equals("new")) {
            mSaveRepButton.setOnClickListener(this);
        } else {
            mSaveRepButton.setVisibility(View.GONE);
        }

        return view;
    }

    public void checkMissedVotesColor() {
        if (Float.parseFloat(mRep.getMissedVotes()) >= 3 ) {
            mRepMissedVotesTextView.setTextColor(Color.RED);
        }
    }

    @Override
    public void onClick(View v) {
        if (v == mSaveRepButton) {
            // create a pushId for Rep and User when it is saved
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            String uid = user.getUid();

            DatabaseReference repRef = FirebaseDatabase
                    .getInstance()
                    .getReference(Constants.FIREBASE_CHILD_SAVED_MEMBERS)
                    .child(uid);  // create the node

            DatabaseReference pushRef = repRef.push();
            String pushId = pushRef.getKey();
            mRep.setPushId(pushId);
            pushRef.setValue(mRep);

            Toast.makeText(getContext(), "Rep Saved", Toast.LENGTH_SHORT).show();
        }
        if (v == mRepPhoneTextView) {
            Intent phoneIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + mRep.getPhone()));
            startActivity(phoneIntent);
        }
        if (v == mRepWebsiteTextView) {
            Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(mRep.getWebsite()));
            startActivity(webIntent);
        }
    }

    private void getVotes(String memberId) {

        final ProPublicaService propublicaService = new ProPublicaService();
        propublicaService.findVotes(memberId, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                mVotes = propublicaService.processVoteResults(response);
                String count = mVotes.size() + "";

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        mVoteAdapter = new VoteListAdapter(getActivity(), mVotes);
                        mVotesRecyclerView.setAdapter(mVoteAdapter);
                        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());  // prev RepDetailFragment.this
                        mVotesRecyclerView.setLayoutManager(layoutManager);
                        mVotesRecyclerView.setHasFixedSize(true);
                    }
                });
            }
        });
    }

}

