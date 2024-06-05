/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package assignment;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author wanab
 */
class Job {
    int jobId;
    Date submitTime;
    Date endTime;
    String partition;

    public Job(int jobId) {
        this.jobId = jobId;
    }
}

public class HPCLogAnalyzer extends JFrame {
    private List<Job> jobs = new ArrayList<>();
    private Map<String, Integer> partitionCounts = new HashMap<>();
    private int errorCount = 0;

    public HPCLogAnalyzer() {
        super("HPC Log Analyzer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLayout(new BorderLayout());

        // Create and add the table
        JTable table = new JTable();
        DefaultTableModel tableModel = new DefaultTableModel();
        tableModel.addColumn("Metric");
        tableModel.addColumn("Value");
        table.setModel(tableModel);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // Create and add the chart panel
        JPanel chartPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                int width = getWidth();
                int height = getHeight();
                int maxCount = partitionCounts.values().stream().max(Integer::compareTo).orElse(1);
                int barWidth = width / partitionCounts.size();

                int i = 0;
                for (Map.Entry<String, Integer> entry : partitionCounts.entrySet()) {
                    int barHeight = (int) ((entry.getValue() / (double) maxCount) * height);
                    g.fillRect(i * barWidth, height - barHeight, barWidth - 10, barHeight);
                    g.drawString(entry.getKey(), i * barWidth + 5, height - barHeight - 5);
                    i++;
                }
            }
        };
        chartPanel.setPreferredSize(new Dimension(800, 300));
        add(chartPanel, BorderLayout.SOUTH);

        try {
            readLogFile("extracted_log.txt");
            displayMetrics(tableModel);
        } catch (IOException | ParseException e) {
        }

        pack();
        setVisible(true);
    }

    public void readLogFile(String filePath) throws IOException, ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        Pattern submitPattern = Pattern.compile("\\[(.+)\\] _slurm_rpc_submit_batch_job: JobId=(\\d+) InitPrio=\\d+ usec=\\d+");
        Pattern allocatePattern = Pattern.compile("\\[(.+)\\] sched: Allocate JobId=(\\d+) NodeList=\\w+ #CPUs=\\d+ Partition=(\\w+)");
        Pattern completePattern = Pattern.compile("\\[(.+)\\] _job_complete: JobId=(\\d+) done");
        Pattern errorPattern = Pattern.compile("\\[(.+)\\] error: This association...");

        try (BufferedReader br = new BufferedReader(new FileReader("C:/Users/wanab/Documents/NetBeansProjects/Assignment/extracted_log"))) {
            String line;
            while ((line = br.readLine()) != null) {
                Matcher submitMatcher = submitPattern.matcher(line);
                Matcher allocateMatcher = allocatePattern.matcher(line);
                Matcher completeMatcher = completePattern.matcher(line);
                Matcher errorMatcher = errorPattern.matcher(line);

                if (submitMatcher.find()) {
                    Date timestamp = sdf.parse(submitMatcher.group(1));
                    int jobId = Integer.parseInt(submitMatcher.group(2));
                    Job job = new Job(jobId);
                    job.submitTime = timestamp;
                    jobs.add(job);
                } else if (allocateMatcher.find()) {
                    int jobId = Integer.parseInt(allocateMatcher.group(2));
                    String partition = allocateMatcher.group(3);
                    for (Job job : jobs) {
                        if (job.jobId == jobId) {
                            job.partition = partition;
                            partitionCounts.put(partition, partitionCounts.getOrDefault(partition, 0) + 1);
                            break;
                        }
                    }
                } else if (completeMatcher.find()) {
                    Date timestamp = sdf.parse(completeMatcher.group(1));
                    int jobId = Integer.parseInt(completeMatcher.group(2));
                    for (Job job : jobs) {
                        if (job.jobId == jobId) {
                            job.endTime = timestamp;
                            break;
                        }
                    }
                } else if (errorMatcher.find()) {
                    errorCount++;
                }
            }
        }
    }

    public void displayMetrics(DefaultTableModel tableModel) {
        tableModel.addRow(new Object[]{"Number of jobs by partitions", ""});
        for (Map.Entry<String, Integer> entry : partitionCounts.entrySet()) {
            tableModel.addRow(new Object[]{entry.getKey(), entry.getValue()});
        }

        tableModel.addRow(new Object[]{"Number of errors", errorCount});

        long totalDuration = 0;
        int completedJobs = 0;
        for (Job job : jobs) {
            if (job.submitTime != null && job.endTime != null) {
                totalDuration += (job.endTime.getTime() - job.submitTime.getTime());
                completedJobs++;
            }
        }
        if (completedJobs > 0) {
            long avgDuration = totalDuration / completedJobs;
            tableModel.addRow(new Object[]{"Average execution time (ms)", avgDuration});
        } else {
            tableModel.addRow(new Object[]{"Average execution time (ms)", "No completed jobs"});
        }
    }

    public static void main(String[] args) {
        new HPCLogAnalyzer();
    }
}
