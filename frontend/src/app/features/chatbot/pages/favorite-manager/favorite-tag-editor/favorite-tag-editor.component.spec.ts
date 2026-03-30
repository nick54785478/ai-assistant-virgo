import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FavoriteTagEditorComponent } from './favorite-tag-editor.component';

describe('FavoriteTagEditorComponent', () => {
  let component: FavoriteTagEditorComponent;
  let fixture: ComponentFixture<FavoriteTagEditorComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FavoriteTagEditorComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(FavoriteTagEditorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
