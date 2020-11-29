package com.crio.qcharm.ds;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

import com.crio.qcharm.request.PageRequest;
import com.crio.qcharm.request.SearchRequest;

import org.apache.commons.lang3.StringUtils;

public class SourceFileVersionLinkedListImpl implements SourceFileVersion {

  String filename;
  List<String> lines = new LinkedList<>();

  SourceFileVersionLinkedListImpl(FileInfo fileInfo) {
    this.filename = fileInfo.getFileName(); 
    this.lines = new LinkedList<>(fileInfo.getLines());
  }

  public SourceFileVersionLinkedListImpl() {
  }

  public SourceFileVersionLinkedListImpl(SourceFileVersionLinkedListImpl obj) {
    this.filename = obj.filename;
    this.lines = new LinkedList<>(obj.lines);
  }

  public SourceFileVersion apply(List<Edits> edits) {
    List<String> lines = new LinkedList<>();
    lines.addAll(lines);
    // SourceFileVersionLinkedListImpl latest = new
    // SourceFileVersionLinkedListImpl();
    for (Edits oneEdit : edits) {
      if (oneEdit instanceof UpdateLines) {
        apply((UpdateLines) oneEdit);
      } else {
        assert (oneEdit instanceof SearchReplace);
        apply((SearchReplace) oneEdit);
      }
    }
    return this;
  }

  public void apply(SearchReplace searchReplace) {
    List<Cursor> answer = new LinkedList<>();
    answer = PatternSearchAlgorithm.searchPattern(searchReplace.getPattern().toCharArray(), this.lines);
    TreeSet<Cursor> ts = new TreeSet<>(Comparator.comparing(Cursor::getLineNo));
    ts.addAll(answer);
    for (Cursor c : ts) {
      String temp = lines.get(c.getLineNo());
      temp = StringUtils.replace(temp, searchReplace.getPattern(), searchReplace.getNewPattern());
      lines.set(c.getLineNo(), temp);
    }
    // int count = 0;
    // for (String s : this.lines) {
    // String temp = s;
    // temp = StringUtils.replace(temp, searchReplace.getPattern(),
    // searchReplace.getNewPattern());
    // lines.set(count, temp);
    // count++;
    // }
  }

  public void apply(UpdateLines updateLines) {
    int start = updateLines.getStartingLineNo();
    int end = start + updateLines.getNumberOfLines();
    List<String> subLines;
    try {
      subLines = this.lines.subList(start, end + 1);
    } catch (Exception e) {
      subLines = this.lines.subList(start, start);
    }
    subLines.clear();
    subLines.addAll(updateLines.getLines());
  }

  @Override
  public Page getLinesBefore(PageRequest pageRequest) {
    int lineNumber = pageRequest.getStartingLineNo();
    int numberOfLines = pageRequest.getNumberOfLines();
    if (lineNumber == 0) {
      return new Page(new LinkedList<String>(), 0, pageRequest.getFileName(), pageRequest.getCursorAt());
    } else if (lineNumber - numberOfLines < 0) {
      return new Page(lines.subList(0, lineNumber), 0, pageRequest.getFileName(), pageRequest.getCursorAt());
    }
    return new Page(lines.subList(lineNumber - numberOfLines, lineNumber), lineNumber - numberOfLines,
        pageRequest.getFileName(), pageRequest.getCursorAt());
  }

  @Override
  public Page getLinesAfter(PageRequest pageRequest) {
    int lineNumber = pageRequest.getStartingLineNo();
    int numberOfLines = pageRequest.getNumberOfLines();
    if (lineNumber >= lines.size()) {
      return new Page(new LinkedList<String>(), lineNumber, pageRequest.getFileName(), pageRequest.getCursorAt());
    } else if (lineNumber + numberOfLines + 1 > lines.size()) {
      return new Page(this.lines.subList(lineNumber + 1, this.lines.size()), lineNumber + 1, pageRequest.getFileName(),
          pageRequest.getCursorAt());
    }
    return new Page(
        lines.subList(lineNumber + 1,
            lineNumber + numberOfLines + 1 > this.lines.size() ? lines.size() : lineNumber + numberOfLines + 1),
        lineNumber + 1, pageRequest.getFileName(), pageRequest.getCursorAt());
  }

  @Override
  public Page getLinesFrom(PageRequest pageRequest) {
    int lineNumber = pageRequest.getStartingLineNo();
    int numberOfLines = pageRequest.getNumberOfLines();
    return new Page(
        lines.subList(lineNumber,
            lines.size() < lineNumber + numberOfLines ? lines.size() : lineNumber + numberOfLines),
        lineNumber, pageRequest.getFileName(), new Cursor(lineNumber, 0));
  }

  public List<Cursor> getCursors(SearchRequest searchRequest) {
    List<Cursor> answer = new LinkedList<>();
    try {
      answer = PatternSearchAlgorithm.searchPattern(searchRequest.getPattern().toCharArray(), this.lines);
    } catch (Exception e) {
      return new LinkedList<>();
    }
    return answer;
  }

  @Override
  public List<String> getAllLines() {
    return this.lines;
  }

  @Override
  public Page getCursorPage() {
    return null;
  }

}
