library(XML)
library(gtools)






calculateBandwidthAgent <- function(agent){
         i = xmlChildren(agent)[c("interface")]	
	 ab = xmlGetAttr(i[[1]],"avalibleBandwidth")
	 mb = xmlGetAttr(i[[1]],"maxBandwidth")
	 ab = as.double(ab)
	 mb = as.double(mb)
	 return (mb-ab)
}

calculateBandwidthStep <- function(step){
        agents = getNodeSet(step, "//agent")
	total = 0.0
	for (agent in agents){
	    total = total + calculateBandwidthAgent(agent)
	}
	return (total)
	}

calcSatisfaction <- function(step){
		 agents = getNodeSet(step, "//agent")
		  total = 0.0
		  for(agent in agents){
		     ua = xmlChildren(agent)[c("userAgent")]
		     s = xmlGetAttr(ua[[1]],"satisfied")	
		     if("true" == s){
		     total = total + 1.0
		     }		  
		  }
		  return ((total/length(agents))*100.0)
      }

countAgents <- function(step){
		 agents = getNodeSet(step, "//agent")		 
		  return (length(agents))
      }


mav <- function(x,n=5){filter(x,rep(1/n,n), sides=2)}




bandwidth = c()
satisfaction = c()
agents = c()

setwd("/home/andyroid/uni/cs4526/Application/test/control/data/chart")


files = list.files()
files = mixedsort(files)

for(file in files){
xml = xmlInternalTreeParse(file)


steps = getNodeSet(xml, "//Step")


for(step in steps){
	bandwidth[length(bandwidth)+1] <- calculateBandwidthStep(step)	 
	satisfaction[length(satisfaction)+1] <- calcSatisfaction(step)
	agents[length(agents)+1] <- countAgents(step)
}
}


total = 0
for(a in agents){
      total = total + a
}
averageAgents = total/length(agents)
print (averageAgents)


setwd("/home/andyroid/uni/cs4526/Application/test/control")






pdf(file="bandwidth.pdf", height=6, width=10)
par(mar=c(4, 5, 0.3, 5))

#time calc
timeseq <- seq(0,ceiling(length(bandwidth)/60),1)
timeLabels = pretty(range(timeseq),15)
time = timeLabels*60


#bandwidth calc
bwseq <- seq(0,ceiling(max(bandwidth)/1024),1)
bwlabels = pretty(range(bwseq),10)
bw = bwlabels*1024

#Bandwidth
plot(bandwidth, axes=F, ylim=c(0,max(bandwidth)), xlab="", ylab="",type="l",col="blue", main="")
axis(4, at=bw, labels=bwlabels,  col="black",lwd=1,las=1)
mtext(4,text="Mbps",line=2.5)

#Percentage
par(new=T)
perc <- seq(0,100,10)
plot(satisfaction, axes=F, ylim=c(0,100), xlab="", ylab="",type="l",col="red",main="")
axis(2, at=perc,labels=sprintf("%s%%", perc),las=1)
#mtext(2,text="Satisfied",line=2.5)

#Time
axis(1,at=time,labels=timeLabels)
mtext("Simulation Time (min)",side=1,col="black",line=2)

legend(x=120,y=20,legend=c("% Satisfied Devices","Bandwidth"),lty=c(1,1),col=c("red","blue"))

dev.off()



print(bandwidth)
print(satisfaction)




