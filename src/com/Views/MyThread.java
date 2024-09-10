package com.Views;

import com.GridTopologyAnalysis.Analysis;

public class MyThread implements Runnable {
    private boolean workFlag = true;  // 标志是否继续工作
    private final Analysis model;
    private final DrawPanel panel;
    private final StationView view;

    public MyThread(Analysis model, DrawPanel panel, StationView view) {
        this.model = model;
        this.panel = panel;
        this.view = view;
    }

    public void setWorkFlag(boolean flag) {
        this.workFlag = flag;
    }

    @Override
    public void run() {
        System.out.println("Work flag status: " + this.workFlag);
        while (this.workFlag) {
            // 初始化模型
            initializeModelIfNecessary();

            // 如果没有选择站点，则等待
            if (this.panel.getStationNo() == -1) {
                waitForStationSelection();
                continue;
            }

            // 更新数据和UI
            updateMeasurementsAndUI();
        }
    }

    private void initializeModelIfNecessary() {
        if (!this.model.isInit()) {
            this.model.init();
        }
    }

    private void waitForStationSelection() {
        try {
            java.lang.Thread.sleep(3000);  // 等待3秒
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void updateMeasurementsAndUI() {
        System.out.println("\nUpdating measurements from memcached...");
        this.model.updateMeas(this.panel.getDeviceSet());
        this.model.buildTopo();

        System.out.println("Updating data units from measurements...");
        this.panel.updateDataUnit();

        System.out.println("Updating UI...");
        this.panel.updateUI();
        this.view.updateTopo();
        System.out.println("Finished updating UI.");

        try {
            java.lang.Thread.sleep(2000);  // 每次更新后等待2秒
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 安全地让当前线程休眠指定的毫秒数，并处理中断异常。
     * @param millis 休眠的时间，单位为毫秒
     */
    static void safeSleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            // 捕获中断异常，并打印堆栈跟踪
            System.err.println("Thread was interrupted during sleep: " + e.getMessage());
            Thread.currentThread().interrupt();  // 恢复线程的中断状态
        }
    }
}

