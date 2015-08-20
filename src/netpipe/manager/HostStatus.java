package netpipe.manager;

import java.util.List;

public class HostStatus {
    private double load;
    private double maxLoad;
    private double cpuUseRate;
    private List<Cpu> cpus;
    private int totalMemory;
    private int freeMemory;
    private int netCapacity;
    private int netLoad;
    private int totalDiskSpace; // M
    private int freeDiskSpace; // M
    
    public double getCpuUseRate() {
        return cpuUseRate;
    }

    public void setCpuUseRate(double cpuUseRate) {
        this.cpuUseRate = cpuUseRate;
    }

    public List<Cpu> getCpus() {
        return cpus;
    }

    public void setCpus(List<Cpu> cpus) {
        this.cpus = cpus;
    }

    public int getTotalMemory() {
        return totalMemory;
    }

    public void setTotalMemory(int totalMemory) {
        this.totalMemory = totalMemory;
    }

    public int getFreeMemory() {
        return freeMemory;
    }

    public void setFreeMemory(int freeMemory) {
        this.freeMemory = freeMemory;
    }

    public int getNetCapacity() {
        return netCapacity;
    }

    public void setNetCapacity(int netCapacity) {
        this.netCapacity = netCapacity;
    }

    public int getNetLoad() {
        return netLoad;
    }

    public void setNetLoad(int netLoad) {
        this.netLoad = netLoad;
    }

    public int getTotalDiskSpace() {
        return totalDiskSpace;
    }

    public void setTotalDiskSpace(int totalDiskSpace) {
        this.totalDiskSpace = totalDiskSpace;
    }

    public int getFreeDiskSpace() {
        return freeDiskSpace;
    }

    public void setFreeDiskSpace(int freeDiskSpace) {
        this.freeDiskSpace = freeDiskSpace;
    }

    public double getLoad() {
        return load;
    }

    public void setLoad(double load) {
        this.load = load;
    }

    public double getMaxLoad() {
        return maxLoad;
    }

    public void setMaxLoad(double maxLoad) {
        this.maxLoad = maxLoad;
    }
}
