import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FavoriteManagerComponent } from './favorite-manager.component';

describe('FavoriteManagerComponent', () => {
  let component: FavoriteManagerComponent;
  let fixture: ComponentFixture<FavoriteManagerComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FavoriteManagerComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(FavoriteManagerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
