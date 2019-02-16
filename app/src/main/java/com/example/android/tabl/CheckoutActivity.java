package com.example.android.tabl;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.Button;
import android.content.Intent;
import android.view.View;

import com.example.android.tabl.basket_checkout_recyclerview.BasketCheckout;
import com.example.android.tabl.basket_checkout_recyclerview.BasketCheckoutAdapter;
import com.example.android.tabl.utils.RecyclerItemClickListener;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO: NEED TO MAKE HELP ACTIVITY AND CLASS SO I CAN LINK THIS BUTTON TO IT, LINKED IT TO 'MORE' PAGE FOR NOW
 */

public class CheckoutActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private BasketCheckoutAdapter basketCheckoutAdapter;
    private List<BasketCheckout> checkoutList = new ArrayList<>();
    private final String TABLE_TITLE = "Table ";

    private Button cancelOrderButton, HelpWithOrderButton, addTipButton;


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        checkoutList = new ArrayList<>();
        recyclerView = findViewById(R.id.checkoutRecyclerView);
        recyclerView.addOnItemTouchListener(
                new RecyclerItemClickListener(this,recyclerView,
                        new RecyclerItemClickListener.OnItemClickListener() {
                            @Override public void onItemClick(View view, int position) {
                                checkoutList.get(position);
                                //go back to basket/checkout view, passing table data
                                //TODO: update to above comment
                                finish();
                            }
                            @Override public void onLongItemClick(View view, int position) {
                            }
                        })
        );

        basketCheckoutAdapter = new BasketCheckoutAdapter(checkoutList);
        RecyclerView.LayoutManager tLayoutManager = new LinearLayoutManager(
                getApplicationContext());
        recyclerView.setLayoutManager(tLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(basketCheckoutAdapter);
        prepTableData();
    }

    public void prepTableData(){
        int numOfTables = getNumOfTables();
        for(int i = 1; i <= numOfTables; i++){
            checkoutList.add(new BasketCheckout(TABLE_TITLE+i));
        }
        basketCheckoutAdapter.notifyDataSetChanged();


        // Cancel Order button will open BasketActivity
        cancelOrderButton = findViewById(R.id.CancelOrderButton);
        cancelOrderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), BasketActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        });

        HelpWithOrderButton = findViewById(R.id.HelpWithOrderButton);
        HelpWithOrderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), MoreActivity.class); //HERE^
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        });





}
}


