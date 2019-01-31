#############################################################################
### bmscaf_analysis.R - analyze and summarize bmscaf scaffold data.
### Input: CSV 
### smiles,freq,scafid
### 
### Jeremy Yang
###  11 Nov 2013
#############################################################################

args <- commandArgs(trailingOnly=TRUE)
for (i in 1:length(args)) { print(sprintf("args[%d]: %s",i,args[i])) }

if (length(args)>0) { IFILE <- args[1] } else { 
  #IFILE <- paste(Sys.getenv("HOME"),"/projects/bmscaf/data/bmscaf_dedup_cbscafid_indb_sorted.smi",sep="")
  IFILE <- paste(Sys.getenv("HOME"),"/Dropbox/projects/CarlsbadOne/SHARED/bmscaf_app_cbscafid_indb_sorted.smi",sep="")
  #stop("Input file must be specified, format: SMILES FREQ ID") 
}


## read dataframe
print(sprintf("IFILE: %s",IFILE))
scafs <- read.delim(IFILE,header=FALSE,sep="",quote="",colClasses="character")

nscaf <- nrow(scafs)
print(sprintf("Nscaf: %d",nscaf))

names(scafs) <- c("smiles","freq","scafid")


## fix datatypes of interest
scafs[["freq"]] <- as.numeric(scafs[["freq"]])

## Report some means, std, quantiles.
for (tag in c("freq"))
{
  vals <- scafs[[tag]]
  print(sprintf("%s max: %.2f min: %.2f",tag,max(vals),min(vals)))
  print(sprintf("%s mean: %.2f stddev: %.2f",tag,mean(vals),sd(vals)))
  qs <- quantile(vals, probs = c(.50, .75, .80, .85, .90, .95, .97, .99))
  for (name in names(qs))
  {
    print(sprintf("%s %s quantile: %8.2f",tag,name,qs[[name]]))
  } 
}

sum_freq <- sum(scafs[["freq"]])
print(sprintf("sum of freqs: %d",sum_freq))

cumfreq <- cumsum(scafs[["freq"]])

freq_scale <- 1e6
cumfreq <- cumfreq/freq_scale
sum_freq <- sum_freq/freq_scale

xmax <- 10000
plot(y = cumfreq[1:xmax], x = 1:xmax, ylim=c(0,sum_freq*1.05),
     ylab = sprintf("frequency (%.1e)",freq_scale),
     xlab = "i_scaf, decreasing freq order")
abline(h=sum_freq,col="red",lty=3)
text(x = xmax*0.75, y = sum_freq,pos=1, col="red", sprintf("%.2f (100%%)",sum_freq))
abline(h=sum_freq/2,col="red",lty=3)
text(x = xmax*0.75, y = sum_freq/2,pos=1, col="red", sprintf("%.2f (50%%)",sum_freq/2))
abline(v=nscaf/100,col="red",lty=3)
text(x=nscaf/100,y=sum_freq*0.25,pos=2,srt=90,col="red",sprintf("%.2f (1%%)",nscaf/100))
title("Cumulative scaffold frequencies in PubChem",
      sub=sprintf("total scaffold count: %d",nscaf))

