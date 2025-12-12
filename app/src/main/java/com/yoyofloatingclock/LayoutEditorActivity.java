package com.yoyofloatingclock;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

/**
 * 自定义布局编辑器Activity
 */
public class LayoutEditorActivity extends AppCompatActivity {
    
    private GridEditorView gridEditor;
    private SeekBar seekRows, seekColumns;
    private TextView tvRows, tvColumns, tvCellCount;
    private Button btnMerge, btnDelete, btnAutoFill, btnReset, btnSave;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_layout_editor);
        
        initViews();
        setupListeners();
    }
    
    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        
        gridEditor = findViewById(R.id.grid_editor);
        seekRows = findViewById(R.id.seek_rows);
        seekColumns = findViewById(R.id.seek_columns);
        tvRows = findViewById(R.id.tv_rows);
        tvColumns = findViewById(R.id.tv_columns);
        tvCellCount = findViewById(R.id.tv_cell_count);
        
        btnMerge = findViewById(R.id.btn_merge);
        btnDelete = findViewById(R.id.btn_delete);
        btnAutoFill = findViewById(R.id.btn_auto_fill);
        btnReset = findViewById(R.id.btn_reset);
        btnSave = findViewById(R.id.btn_save);
        
        updateCellCount();
    }
    
    private void setupListeners() {
        // 行数调节
        seekRows.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvRows.setText(String.valueOf(progress));
                if (fromUser) {
                    gridEditor.setGridSize(progress, seekColumns.getProgress());
                    updateCellCount();
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        // 列数调节
        seekColumns.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvColumns.setText(String.valueOf(progress));
                if (fromUser) {
                    gridEditor.setGridSize(seekRows.getProgress(), progress);
                    updateCellCount();
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        // 合并按钮
        btnMerge.setOnClickListener(v -> {
            if (gridEditor.mergeSelectedCells()) {
                Toast.makeText(this, "合并成功", Toast.LENGTH_SHORT).show();
                updateCellCount();
            } else {
                Toast.makeText(this, "请选择至少2个相邻的格子形成矩形", Toast.LENGTH_SHORT).show();
            }
        });
        
        // 删除按钮
        btnDelete.setOnClickListener(v -> {
            gridEditor.deleteLastCell();
            Toast.makeText(this, "已删除最后一个格子", Toast.LENGTH_SHORT).show();
            updateCellCount();
        });
        
        // 自动填充按钮
        btnAutoFill.setOnClickListener(v -> {
            gridEditor.autoFill();
            Toast.makeText(this, "已自动填充剩余空格", Toast.LENGTH_SHORT).show();
            updateCellCount();
        });
        
        // 重置按钮
        btnReset.setOnClickListener(v -> {
            gridEditor.reset();
            Toast.makeText(this, "已重置", Toast.LENGTH_SHORT).show();
            updateCellCount();
        });
        
        // 保存按钮
        btnSave.setOnClickListener(v -> saveLayout());
    }
    
    private void updateCellCount() {
        int count = gridEditor.getCellCount();
        tvCellCount.setText(String.format("当前格子数：%d", count));
    }
    
    private void saveLayout() {
        List<GridCell> cells = gridEditor.getCells();
        
        if (cells.isEmpty()) {
            Toast.makeText(this, "请先创建布局", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 返回创建的布局
        Intent result = new Intent();
        result.putExtra("cell_count", cells.size());
        result.putParcelableArrayListExtra("grid_cells", new ArrayList<>(cells));
        setResult(Activity.RESULT_OK, result);
        finish();
    }
}
