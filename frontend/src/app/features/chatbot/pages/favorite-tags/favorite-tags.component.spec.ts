import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FavoriteTagsComponent } from './favorite-tags.component';

describe('FavoriteTagsComponent', () => {
  let component: FavoriteTagsComponent;
  let fixture: ComponentFixture<FavoriteTagsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FavoriteTagsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(FavoriteTagsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
