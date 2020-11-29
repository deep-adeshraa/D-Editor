package com.crio.qcharm.ds;

import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import com.crio.qcharm.request.EditRequest;
import com.crio.qcharm.request.PageRequest;
import com.crio.qcharm.request.SearchReplaceRequest;
import com.crio.qcharm.request.SearchRequest;
import com.crio.qcharm.request.UndoRequest;

public class SourceFileHandlerLinkedListImpl implements SourceFileHandler {

  SourceFileVersionLinkedListImpl sourceFileVersionLinkedListImpl;
  List<String> lines;
  Stack<SourceFileVersionLinkedListImpl> undoStack = new Stack<>();
  Stack<SourceFileVersionLinkedListImpl> redoStack = new Stack<>();

  public SourceFileHandlerLinkedListImpl(String fileName) {
  }

  @Override
  public SourceFileVersion getLatestSourceFileVersion(String fileName) {
    return null;
  }

  @Override
  public Page loadFile(FileInfo fileInfo) {
    this.sourceFileVersionLinkedListImpl = new SourceFileVersionLinkedListImpl(fileInfo);
    this.lines = sourceFileVersionLinkedListImpl.getAllLines();

    return new Page(lines.size() <= 50 ? lines : lines.subList(0, 50), 0, fileInfo.getFileName(), new Cursor(0, 0));
  }

  @Override
  public Page getPrevLines(PageRequest pageRequest) {
    return this.sourceFileVersionLinkedListImpl.getLinesBefore(pageRequest);
  }

  @Override
  public Page getNextLines(PageRequest pageRequest) {
    return this.sourceFileVersionLinkedListImpl.getLinesAfter(pageRequest);
  }

  @Override
  public Page getLinesFrom(PageRequest pageRequest) {
    return this.sourceFileVersionLinkedListImpl.getLinesFrom(pageRequest);
  }

  @Override
  public List<Cursor> search(SearchRequest searchRequest) {
    List<Cursor> answer = new LinkedList<>();
    answer = this.sourceFileVersionLinkedListImpl.getCursors(searchRequest);
    return answer;
  }

  CopyBuffer copyBuffer;

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
    return new SourceFileVersionLinkedListImpl((SourceFileVersionLinkedListImpl) ver);
  }

  @Override
  public void editLines(EditRequest editRequest) {
    this.undoStack.push(new SourceFileVersionLinkedListImpl(this.sourceFileVersionLinkedListImpl));
    int start = editRequest.getStartingLineNo();
    int end = editRequest.getEndingLineNo();
    UpdateLines update = new UpdateLines(start, end - start - 1, editRequest.getNewContent(),
        editRequest.getCursorAt());
    this.sourceFileVersionLinkedListImpl.apply(update);
  }

  @Override
  public void searchReplace(SearchReplaceRequest searchReplaceRequest) {
    this.undoStack.push(new SourceFileVersionLinkedListImpl(this.sourceFileVersionLinkedListImpl));
    SearchReplace searchRequest = new SearchReplace(searchReplaceRequest.getStartingLineNo(),
        searchReplaceRequest.getLength(), new Cursor(), searchReplaceRequest.getPattern(),
        searchReplaceRequest.getNewPattern());
    this.sourceFileVersionLinkedListImpl.apply(searchRequest);
  }

  @Override
  public void undo(UndoRequest undoRequest) {
    if (!this.undoStack.isEmpty()) {
      redoStack.push(this.sourceFileVersionLinkedListImpl);
      this.sourceFileVersionLinkedListImpl = this.undoStack.pop();
    }
  }

  @Override
  public void redo(UndoRequest undoRequest) {
    if (!this.undoStack.isEmpty()) {
      undoStack.push(this.sourceFileVersionLinkedListImpl);
      this.sourceFileVersionLinkedListImpl = this.redoStack.pop();
    }
  }

  public Page getCursorPage() {
    return null;
  }

}
