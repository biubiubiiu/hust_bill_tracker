package com.example.hustbillbook.activity;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.example.hustbillbook.R;
import com.example.hustbillbook.SingleCommonData;
import com.example.hustbillbook.DataRepository;
import com.example.hustbillbook.adaptor.ViewPagerAdaptor;
import com.example.hustbillbook.adaptor.TypeRecycleAdaptor;
import com.example.hustbillbook.bean.AccountBean;
import com.example.hustbillbook.bean.RecordBean;
import com.example.hustbillbook.bean.TypeViewBean;
import com.example.hustbillbook.tools.CalendarUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class AddRecordActivity extends AppCompatActivity implements View.OnClickListener {

    private int handleType; //三种情况下的处理，
    //1，简单的添加，执行原操作即可
    //2，对应账户下的添加，此时账户为不可选择
    //3，对原记录的修改，需要将原记录内容复现
    //4,对应账户下的修改，需同时将原内容复现和账户设为不可选
    /*modified on 6/30*/

    private int accountNum; //记录属于哪个账户

    private boolean isExpense;

    private ViewPager viewPager;
    private LinearLayout indicator; // 分页标识
    private TextView expenseTv; // 支出按钮
    private TextView incomeTv;  // 收入按钮
    private ImageView backIv;   // 返回按钮
    private EditText titleEt;   // 账单备注
    private TextView accountTv; // 选择账户按钮

    private TextView typeTv;    // 显示账单类别
    private TextView moneyTv;   // 显示账单金额
    private TextView dateTv;    // 选择日期按钮
    private TextView commitTv; // 提交按钮

    // viewPager 相关
    private int page;
    private List<TypeViewBean> typeList;    // 提供 viewPager 的适配器使用
    private List<TypeViewBean> expenseTypeList;
    private List<TypeViewBean> incomeTypeList;
    private ImageView[] indicatorIcons;
    private List<View> viewList;
    private int selectedType;    // 记录所选择的类别;

    private final int SPAN_COUNT = 4;   // viewpager 中一行显示的分类个数
    private final int ITEMS_PER_PAGE = SPAN_COUNT * 3; // viewpager 中一页显示的类别个数
    private final int MAX_INTEGER_LENGTH = 8;   // 输入金额中，整数部分的最大长度
    private final int MAX_DECIMAL_LENGTH = 2;   // 输入金额中，小数部分的最大长度

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_record);

        initHandleType();
        initData();
        initMapping();
        initStatus();
        initWidget();
    }

    private void initHandleType() {
        Intent intent = getIntent();
        handleType = Objects.requireNonNull(intent.getIntExtra("handleType", 1));
    }

    @Override
    protected void onStart() {
        super.onStart();
        initViewPager();
    }

    /*
     设置点击行为的监听器
     */
    private void initAction() {
        incomeTv.setOnClickListener(this);
        expenseTv.setOnClickListener(this);
        dateTv.setOnClickListener(this);
        commitTv.setOnClickListener(this);
        backIv.setOnClickListener(this);
        accountTv.setOnClickListener(this);
    }

    private void initMapping() {
        viewPager = findViewById(R.id.vp_types);
        indicator = findViewById(R.id.layout_icons);
        expenseTv = findViewById(R.id.tv_expense);
        incomeTv = findViewById(R.id.tv_income);
        backIv = findViewById(R.id.btn_back);
        typeTv = findViewById(R.id.tv_record_type);
        moneyTv = findViewById(R.id.tv_record_money);
        dateTv = findViewById(R.id.tv_record_date);
        commitTv = findViewById(R.id.tv_btn_commit);
        accountTv = findViewById(R.id.tv_account);
        titleEt = findViewById(R.id.et_record_title);
    }

    /*
     初始化窗口组件
     */
    private void initWidget() {
        // 默认选择“消费”页的第一个分类
        typeTv.setText(expenseTypeList.get(0).getTypeName());

        switch (handleType) {
            case 1:
                AccountBean initAccount = SingleCommonData.accountAt(0);//获取第一个账户
                accountTv.setText(initAccount.accountName);
                accountNum = 0;
                break;
            case 2:
                accountNum = Objects.requireNonNull(getIntent().getExtras()).getInt("accountNum", 0);
                accountTv.setText(SingleCommonData.accountAt(accountNum).accountName);
                break;
            /*modified on 6/30*/
            case 4:     //第四种情况，直接使用case3的代码
            case 3:
                int index = getIntent().getIntExtra("index", 0);
                RecordBean recordBean = SingleCommonData.recordAt(index);

                accountNum = recordBean.recordAccount;
                accountTv.setText(SingleCommonData.accountAt(accountNum).accountName);

                isExpense = recordBean.isExpense;
                setStatus();

                moneyTv.setText(recordBean.recordMoney);
                typeTv.setText(DataRepository.getInstance().findType(recordBean.recordType).getTypeName());//TODO
                selectedType = recordBean.recordType;
                titleEt.setText(recordBean.recordTitle);

                String today = CalendarUtils.getCurrentDate();
                if (recordBean.recordDate.equals(today))
                    dateTv.setText(getString(R.string.today));
                else
                    dateTv.setText(recordBean.recordDate);

                break;
        }
        initAction();
    }

    /*
     初始化 viewpager
     */
    private void initViewPager() {
        // init viewPager
        LayoutInflater inflater = this.getLayoutInflater(); // 获得一个视图管理器
        viewList = new ArrayList<>();   //创建一个存放view的集合对象

        page = (int) Math.ceil(typeList.size() * 1.0 / ITEMS_PER_PAGE);  // 计算需要显示的页数

        for (int i = 0; i < page; i++) {
            List<TypeViewBean> tmpList = new ArrayList<>();
            View view = inflater.inflate(R.layout.item_types_page, viewPager, false);

            // 每一页使用 RecyclerView
            RecyclerView recyclerView = view.findViewById(R.id.page_type_recycle);

            // 将每一页需要显示的分类添加到List
            for (int j = 0; j < ITEMS_PER_PAGE && (i * ITEMS_PER_PAGE + j) < typeList.size(); j++)
                tmpList.add(typeList.get(i * ITEMS_PER_PAGE + j));

            final TypeRecycleAdaptor adaptor = new TypeRecycleAdaptor(this, tmpList);
            adaptor.setOnClickListener(new TypeRecycleAdaptor.OnClickListener() {
                @Override
                public void OnClick(int index) {
                    // 获取实际 index
                    index = index + viewPager.getCurrentItem() * ITEMS_PER_PAGE;
                    typeTv.setText(typeList.get(index).getTypeName());
                    selectedType = typeList.get(index).getId();
                }
            });

            // RecyclerView 实现 GridView 效果
            GridLayoutManager layoutManager = new GridLayoutManager(this, SPAN_COUNT);
            recyclerView.setLayoutManager(layoutManager);
            recyclerView.setAdapter(adaptor);
            viewList.add(view);
        }

        viewPager.setAdapter(new ViewPagerAdaptor(viewList));
        viewPager.setOverScrollMode(View.OVER_SCROLL_NEVER);
        viewPager.setOffscreenPageLimit(1); // 预加载数据页
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                try {
                    for (int i = 0; i < viewList.size(); i++) {
                        indicatorIcons[i].setImageResource(R.drawable.icon_banner_point2);
                    }
                    indicatorIcons[position].setImageResource(R.drawable.icon_banner_point1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
        // 初始化底部分页指示器
        initIcon();
    }

    /*
     初始化所有分类数据
     */
    private void initData() {
        page = 0;

        DataRepository r = DataRepository.getInstance();
        expenseTypeList = r.getAllExpenseTypes();
        incomeTypeList = r.getAllIncomeTypes();

        selectedType = expenseTypeList.get(0).getId();
    }

    /*
     初始化分类页面指示器
     */
    private void initIcon() {
        indicatorIcons = new ImageView[viewList.size()];
        indicator.removeAllViews();
        for (int i = 0; i < indicatorIcons.length; i++) {
            indicatorIcons[i] = new ImageView(this);
            indicatorIcons[i].setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            indicatorIcons[i].setImageResource(R.drawable.icon_banner_point2);
            if (viewPager.getCurrentItem() == i) {
                indicatorIcons[i].setImageResource(R.drawable.icon_banner_point1);
            }
            indicatorIcons[i].setPadding(5, 0, 5, 0);
            indicatorIcons[i].setAdjustViewBounds(true);
            indicator.addView(indicatorIcons[i]);
        }
    }

    /*
     初始化顶部消费和收入按钮的状态
     */
    private void initStatus() {
        isExpense = true;
        setStatus();
    }

    /*
     设置顶部消费和收入按钮的状态
     */
    private void setStatus() {
        if (isExpense) {
            expenseTv.setSelected(true);
            incomeTv.setSelected(false);
            typeList = expenseTypeList;
            selectedType = 1;
            typeTv.setText(DataRepository.getInstance().findType(selectedType).getTypeName());
        } else {
            expenseTv.setSelected(false);
            incomeTv.setSelected(true);
            typeList = incomeTypeList;
            selectedType = 4;
            typeTv.setText(DataRepository.getInstance().findType(selectedType).getTypeName());
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_back:
                this.finish();
                break;
            case R.id.tv_income:
                isExpense = false;
                setStatus();
                initViewPager();
                break;
            case R.id.tv_expense:
                isExpense = true;
                setStatus();
                initViewPager();
                break;
            case R.id.tv_record_date:
                showDataPickDlg();
                break;
            case R.id.tv_calc_btn_dot:
                handleDotInput();
                break;
            case R.id.tv_calc_btn_backspace:
                handleBackSpaceInput();
                break;
            case R.id.tv_account:
                switchAccount();
                break;
            case R.id.tv_btn_commit:
                commit();
                break;
            case R.id.tv_calc_btn1:
                handleNumInput(1);
                break;
            case R.id.tv_calc_btn2:
                handleNumInput(2);
                break;
            case R.id.tv_calc_btn3:
                handleNumInput(3);
                break;
            case R.id.tv_calc_btn4:
                handleNumInput(4);
                break;
            case R.id.tv_calc_btn5:
                handleNumInput(5);
                break;
            case R.id.tv_calc_btn6:
                handleNumInput(6);
                break;
            case R.id.tv_calc_btn7:
                handleNumInput(7);
                break;
            case R.id.tv_calc_btn8:
                handleNumInput(8);
                break;
            case R.id.tv_calc_btn9:
                handleNumInput(9);
                break;
            case R.id.tv_calc_btn0:
                handleNumInput(0);
                break;
        }
    }

    private void commit() {
        if (Double.valueOf(moneyTv.getText().toString()) == 0) {
            Toast.makeText(this, "金额不能为0！", Toast.LENGTH_SHORT).show();
            return;
        }

        int index;
        RecordBean newRecord = new RecordBean();

        newRecord.recordType = selectedType;
        newRecord.recordMoney = moneyTv.getText().toString();
        newRecord.recordTitle = titleEt.getText().toString();
        newRecord.isExpense = isExpense;
        newRecord.recordAccount = accountNum;
        if (dateTv.getText().toString().equals("今天")) {
            newRecord.recordDate = CalendarUtils.getCurrentDate();
        } else {
            newRecord.recordDate = dateTv.getText().toString();
        }

        /*modified on 6/30*/
        if (handleType < 3) {  // 新增记录,对应1，2种情况
            SingleCommonData.addRecord(newRecord);
            index = SingleCommonData.getRecordList().indexOf(newRecord);
        } else {
            index = getIntent().getIntExtra("index", -1);
            RecordBean oldRecord = SingleCommonData.recordAt(index);

            /*modified on 6/30*/
            //处理余额时先恢复旧纪录的钱，再算新纪录的钱
            String s = SingleCommonData.accountAt(accountNum).accountMoney;
            Double d;
            if (oldRecord.isExpense)
                d = Double.valueOf(s) + Double.valueOf(oldRecord.recordMoney);
            else
                d = Double.valueOf(s) - Double.valueOf(oldRecord.recordMoney);
            SingleCommonData.updateAccountMoney(accountNum, d);

            SingleCommonData.updateRecord(oldRecord, newRecord);
        }

        //处理账户余额
        String aM = SingleCommonData.accountAt(accountNum).accountMoney;//账户余额
        double leftMoney;
        if (newRecord.isExpense)
            leftMoney = Double.valueOf(aM) - Double.valueOf(newRecord.recordMoney);
        else
            leftMoney = Double.valueOf(aM) + Double.valueOf(newRecord.recordMoney);

        SingleCommonData.updateAccountMoney(accountNum, leftMoney);

        Toast.makeText(this, "记账成功！", Toast.LENGTH_SHORT).show();

        /*modified on 6/30*/
        Intent intent = new Intent();
        //int in = SingleCommonData.getRecordList().indexOf(newRecord);
        intent.putExtra("index", index);
        //传回新纪录的index，防止用到

        if (handleType == 2 || handleType == 4) {
            Intent intent1 = new Intent();
            intent1.putExtra("index", index);

            setResult(RESULT_OK, intent);
        }

        this.finish();
    }

    private void handleNumInput(int num) {
        String curr = moneyTv.getText().toString();
        if ((curr.equals("0.00") || curr.equals("0")) && num != 0) {
            moneyTv.setText(String.valueOf(num));
            return;
        }

        int dotIndex = curr.indexOf(".");
        if (dotIndex == -1) {
            if (curr.length() < MAX_INTEGER_LENGTH)
                moneyTv.setText(String.format(Locale.US, "%s%d", curr, num));
        } else {
            if (curr.length() - 1 - dotIndex < MAX_DECIMAL_LENGTH)
                moneyTv.setText(String.format(Locale.US, "%s%d", curr, num));
        }
    }

    private void switchAccount() {
        /*modified on 6/30*/
        if (handleType == 2 || handleType == 4)
            return;
        Intent intent = new Intent(AddRecordActivity.this, SelectAccountActivity.class);
        startActivityForResult(intent, 1);
    }

    private void handleBackSpaceInput() {
        String curr = moneyTv.getText().toString();
        if (curr.length() == 1)
            moneyTv.setText("0");
        else
            moneyTv.setText(curr.substring(0, curr.length() - 1));
    }

    private void handleDotInput() {
        String curr = moneyTv.getText().toString();
        String result;
        if (curr.contains("."))
            return;

        if (curr.equals("0.00"))
            result = "0.";
        else
            result = curr + ".";
        moneyTv.setText(result);
    }

    /*
     展示日期选择框
     */
    private void showDataPickDlg() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                AddRecordActivity.this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                String selection = year + "-" + (monthOfYear + 1) + "-" + dayOfMonth;
                if (selection.equals(CalendarUtils.getCurrentDate()))
                    dateTv.setText("今天");
                else
                    dateTv.setText(selection);
            }
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == 1) { // 选择账户返回
            if (resultCode == Activity.RESULT_OK && data != null) {
                int position = data.getIntExtra("position", 0);
                accountTv.setText(SingleCommonData.accountAt(position).accountName);
                accountNum = position;
            }
        }
    }

    /*modified on 6/30*/
    //因为ViewSpecific需要接受一个参数，所以处理一下返回键
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode==KeyEvent.KEYCODE_BACK && (handleType == 2 || handleType == 4)){
            Intent intent = new Intent();
            if (handleType == 2)
                setResult(RESULT_CANCELED);
            else{
                int index = getIntent().getIntExtra("index", -1);
                intent.putExtra("index", index);
                setResult(RESULT_CANCELED, intent);
            }
            this.finish();
        }

        return true;
    }

}
