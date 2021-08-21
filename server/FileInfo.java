public class FileInfo {
    
    protected String fileName;
    protected String fileLastModified;
    protected long fileSize;
    protected boolean fileReads = false;
    protected boolean fileWrites = false;

    FileInfo(String name, String mod, long size, boolean read, boolean write){
        this.fileName = name;
        this.fileLastModified = mod;
        this.fileSize = size;
        this.fileReads = read;
        this.fileWrites = write;
    }

    public String getName(){
        return this.fileName;
    }

    public String getModified(){
        return fileLastModified;
    }

    public long getSize(){
        return fileSize;
    }

    public String getReadWrite(){
        return (this.getRead())&&(this.getWrites()) ? "R/W" : 
            ((this.getRead())&&(!this.getWrites()) ? "R" : 
                (!this.getRead())&&(this.getWrites()) ? "W" : "");
    }

    public boolean getRead(){
        return fileReads;
    }

    public boolean getWrites(){
        return fileWrites;
    }
}
