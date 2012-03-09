package com.anadathur.mvndep

import groovy.transform.ToString

class Dependency{
    String group, artifact, version
    String scope
    List dependsOn = []
    Dependency parent

    def flatString(){
        "${group}:${artifact}:${version}"
    }

    def toString(indent){
        def str = " " * indent + "${group}:${artifact}:${version}"
        if(dependsOn.size() == 0)
            str
        else
            "${str}\n${dependsOn*.toString(indent+1).join("\n")}"
    }

    String toString(){
        return toString(0)
    }

    def addChild(child){
        child.parent = this
        dependsOn.add(child)
    }

    def flatten(){
        def lst = [this]
        lst.addAll((this.dependsOn*.flatten()).flatten())
        lst
    }
}

String.metaClass.asDependency = { ->
    def parts = delegate.trim().split(':')
    if(parts.length == 5){
        return new Dependency(group: parts[0], artifact: parts[1], version: parts[3], scope: parts[4])
    }else {
        return new Dependency(group: parts[0], artifact: parts[1], version: parts[3])
    }
}

def countIndent(str){
    int spaceCount = 0, indx = 0
    while(str[indx++] == " " && indx < str.length())
        ++spaceCount
    return spaceCount
}

def analyze(lines){
    def stack = []
    stack.push([lines[0].asDependency(), countIndent(lines[0])])
    lines.tail().each { line ->
        def nxtIndent = countIndent(line)
        if(nxtIndent > stack.last()[1]){
            addToStack(stack, line, nxtIndent)
        } else{
            while(nxtIndent <= stack.last()[1] && !stack.isEmpty()){
                stack.pop()
            }
            addToStack(stack, line, nxtIndent)
        }
    }
    return stack[0]
}

def addToStack(stack, line, indent){
    def dep = line.asDependency()
    stack.last()[0].addChild(dep)
    stack.push([dep, indent])
}

/*
def analyze(lines, lineNo, indent){
    Dependency dep = lines[lineNo].asDependency()
    while(++lineNo < lines.size()){
        nxtIndent = countIndent(lines[lineNo])
        if (nxtIndent > indent){
            def result = analyze(lines, lineNo, nxtIndent)
            dep.dependsOn.add(result[1])
            lineNo = result[0]
        } else{
            //same indent, parent depends on the current line.
            return [lineNo - 1, dep]
        }
    }
    return [lineNo, dep]
}*/

def analyzeFile(file){
    def lines = new java.io.File(file).readLines()
    def result = analyze(lines)[0]
    println result

    def duplicatesMap = result.flatten().groupBy{"${it.group}:${it.artifact}"}
    println duplicatesMap.collectEntries{k, v -> [k, v*.flatString()]}
    println duplicatesMap.findAll{it.value.size() > 1}
}


analyzeFile("/tmp/dep.txt")