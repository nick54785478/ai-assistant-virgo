import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AiModelCardComponent } from './ai-model-card.component';

describe('AiModelCardComponent', () => {
  let component: AiModelCardComponent;
  let fixture: ComponentFixture<AiModelCardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AiModelCardComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AiModelCardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
