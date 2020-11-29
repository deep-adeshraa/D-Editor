package com.crio.qcharm.ds;

import com.crio.qcharm.request.EditRequest;
import com.crio.qcharm.request.PageRequest;
import com.crio.qcharm.request.SearchReplaceRequest;
import com.crio.qcharm.request.SearchRequest;
import com.crio.qcharm.request.UndoRequest;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

public class SourceFileHandlerHybridImpl implements SourceFileHandler {

  private SourceFileVersion sourceFileVersion;
  private CopyBuffer copyBuffer;
  private Stack<SourceFileVersion> undoStack = new Stack<>();
  private Stack<SourceFileVersion> redoStack = new Stack<>();

  public SourceFileHandlerHybridImpl(String fileName) {
  }

  @Override
  public SourceFileVersion getLatestSourceFileVersion(String fileName) {
    return this.sourceFileVersion;
  }

  @Override
  public Page loadFile(FileInfo fileInfo) {
    this.sourceFileVersion = new SourceFileVersionHybridImpl(fileInfo);
    PageRequest pageRequest = new PageRequest();
    pageRequest.setFileName(fileInfo.getFileName());
    pageRequest.setNumberOfLines(50);
    pageRequest.setStartingLineNo(0);
    return this.sourceFileVersion.getLinesFrom(pageRequest);
  }

  @Override
  public Page getPrevLines(PageRequest pageRequest) {
    return this.sourceFileVersion.getLinesBefore(pageRequest);
  }

  @Override
  public Page getNextLines(PageRequest pageRequest) {
    return this.sourceFileVersion.getLinesAfter(pageRequest);
  }

  @Override
  public Page getLinesFrom(PageRequest pageRequest) {
    return this.sourceFileVersion.getLinesFrom(pageRequest);
  }

  @Override
  public List<Cursor> search(SearchRequest searchRequest) {
    return this.sourceFileVersion.getCursors(searchRequest);
  }

  @Override
  public void setCopyBuffer(CopyBuffer copyBuffer) {
    this.copyBuffer = copyBuffer;
  }

  @Override
  public CopyBuffer getCopyBuffer() {
    return this.copyBuffer;
  }

  @Override
  public SourceFileVersion cloneObj(SourceFileVersion ver) {
    return new SourceFileVersionHybridImpl((SourceFileVersionHybridImpl) ver);
  }

  @Override
  public void editLines(EditRequest editRequest) {
    int start = editRequest.getStartingLineNo();
    int end = editRequest.getEndingLineNo();
    UpdateLines update = new UpdateLines(start, end - start - 1, editRequest.getNewContent(),
        editRequest.getCursorAt());
    push(this.sourceFileVersion);
    this.sourceFileVersion.apply(update);
  }

  @Override
  public void searchReplace(SearchReplaceRequest searchReplaceRequest) {
    SearchReplace replace = new SearchReplace(searchReplaceRequest.getStartingLineNo(),
        searchReplaceRequest.getLength(), new Cursor(searchReplaceRequest.getStartingLineNo(), 0),
        searchReplaceRequest.getPattern(), searchReplaceRequest.getNewPattern());

    push(this.sourceFileVersion);
    this.sourceFileVersion.apply(replace);
  }

  public void push(SourceFileVersion version) {
    undoStack.push(cloneObj(version));
    redoStack.clear();
  }

  @Override
  public void undo(UndoRequest undoRequest) {
    if (undoStack.isEmpty())
      return;
    SourceFileVersion version = undoStack.pop();
    this.redoStack.push(sourceFileVersion);
    this.sourceFileVersion = cloneObj(version);
  }

  @Override
  public void redo(UndoRequest undoRequest) {
    if (redoStack.isEmpty())
      return;
    SourceFileVersion version = redoStack.pop();
    this.undoStack.push(sourceFileVersion);
    this.sourceFileVersion = cloneObj(version);
  }

  public Page getCursorPage() {
    return null;
  }

}