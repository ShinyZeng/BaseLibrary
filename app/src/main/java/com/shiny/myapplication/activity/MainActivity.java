package com.shiny.myapplication.activity;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import com.shiny.myapplication.R;
import com.shiny.myapplication.api.AllGameService;
import com.shiny.myapplication.api.Api;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        initView();


        //OkHttpClient.Builder
//        OkHttpClient.Builder okBuilder = new OkHttpClient.Builder();
//        okBuilder.




        Retrofit retrofit = new Retrofit.Builder().baseUrl(Api.HOST)
                .build();
        AllGameService allGameService = retrofit.create(AllGameService.class);
        //userID=3208442&token=ZRBLQURUQRSEONRUCIDPKOJYESFHARIK&pageNo=0&channel=Migu&pageSize=20&pid=13&versionName=1.8.1.0&version=1810
        int userId = 3208442;
        String token = "ZRBLQURUQRSEONRUCIDPKOJYESFHARIK";
        int pageNo = 0;
        String channel = "Migu";
        int pageSize = 20;
        int pid = 13;
        String versionName = "1.8.1.0";
        int version = 1810;
        Call<String> call = allGameService.getString(userId,token,pageNo,channel,pageSize,pid,versionName,version);

        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                String result = response.body();
                Log.d("MainActivity",result);
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Log.d("MainActivity",t.getMessage());
            }
        });
    }

    private void initView(){
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(2,StaggeredGridLayoutManager.VERTICAL));
//        recyclerView.setAdapter();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
